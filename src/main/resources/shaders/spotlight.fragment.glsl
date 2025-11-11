// Spotlight fragment shader with parallax occlusion mapping and fog
    #ifdef GL_ES
    precision mediump float;
    #endif

    uniform sampler2D u_diffuseTexture;
    uniform sampler2D u_normalTexture;
    uniform sampler2D u_heightTexture;
    uniform float u_heightScale;
    uniform float u_hasHeightMap;

    uniform vec3 u_ambientLight;
    uniform vec3 u_spotPosition;      // Flashlight position in world space
    uniform vec3 u_spotDirection;     // Flashlight direction (normalized)
    uniform vec3 u_spotColor;         // Flashlight color
    uniform float u_spotIntensity;    // Flashlight intensity
    uniform float u_spotCutoff;       // Cosine of inner cone angle
    uniform float u_spotOuterCutoff;  // Cosine of outer cone angle
    uniform float u_spotAttenuation;  // Distance attenuation factor
    uniform vec3 u_cameraPosition;    // Camera/eye position
    uniform vec3 u_moonDirection;     // Directional moonlight
    uniform vec3 u_moonColor;         // Moonlight color

    // Point lights for ceiling lamps (max 20)
    uniform int u_numPointLights;
    uniform vec3 u_pointLightPositions[20];
    uniform vec3 u_pointLightColors[20];
    uniform float u_pointLightIntensities[20];

    // Maze shadow casting
    uniform sampler2D u_mazeTexture;
    uniform float u_mazeWidth;
    uniform float u_mazeHeight;
    uniform float u_cellSize;

    // Fog parameters
    uniform vec3 u_fogColor;          // Fog color (dark)
    uniform float u_fogDensity;       // Fog density
    uniform float u_fogStart;         // Distance where fog starts
    uniform float u_fogEnd;           // Distance where fog is maximum

    varying vec3 v_position;
    varying vec3 v_normal;
    varying vec2 v_texCoord0;
    varying vec3 v_tangent;
    varying vec3 v_bitangent;
    varying vec3 v_viewDir;

    // Shadow ray casting - checks if there's a wall between light and surface
    bool isInShadow(vec3 surfacePos, vec3 lightPos) {
        // Calculate ray from surface to light
        vec3 rayDir = lightPos - surfacePos;
        float rayLength = length(rayDir);
        rayDir = normalize(rayDir);

        // Ray march through the maze grid
        float stepSize = u_cellSize * 0.25; // Quarter cell steps for accuracy
        int maxSteps = int(rayLength / stepSize) + 1;

        for (int step = 1; step < maxSteps; step++) {
            vec3 samplePos = surfacePos + rayDir * (float(step) * stepSize);

            // Convert world position to maze grid coordinates
            float gridX = samplePos.x / u_cellSize;
            float gridZ = samplePos.z / u_cellSize;

            // Check bounds
            if (gridX < 0.0 || gridX >= u_mazeWidth || gridZ < 0.0 || gridZ >= u_mazeHeight) {
                continue;
            }

            // Sample maze texture (walls are white, paths are black)
            vec2 mazeUV = vec2(gridX / u_mazeWidth, gridZ / u_mazeHeight);
            float wallValue = texture2D(u_mazeTexture, mazeUV).r;

            // If we hit a wall, we're in shadow
            if (wallValue > 0.5) {
                return true;
            }
        }

        return false; // No wall blocking, not in shadow
    }

    // Parallax Occlusion Mapping function - conservative version
    vec2 parallaxOcclusionMapping(vec2 texCoords, vec3 viewDir) {
        // Reduce effect based on view angle to prevent extreme distortion
        float viewDotNormal = abs(dot(vec3(0.0, 0.0, 1.0), viewDir));
        if (viewDotNormal < 0.3) {
            return texCoords; // Too shallow angle, skip parallax
        }

        // Conservative layer count
        const float minLayers = 8.0;
        const float maxLayers = 16.0;
        float numLayers = mix(maxLayers, minLayers, viewDotNormal);

        // Calculate size of each layer
        float layerDepth = 1.0 / numLayers;
        float currentLayerDepth = 0.0;

        // Very conservative height scale
        float adjustedScale = u_heightScale * viewDotNormal * 0.5;

        // Amount to shift texture coordinates per layer
        vec2 P = viewDir.xy / max(abs(viewDir.z), 0.3) * adjustedScale;

        // Strict maximum offset to prevent wrapping issues
        float maxOffset = 0.05; // Maximum 5% offset
        P = clamp(P, -maxOffset, maxOffset);

        vec2 deltaTexCoords = P / numLayers;

        // Initial values
        vec2 currentTexCoords = texCoords;
        float currentDepthMapValue = texture2D(u_heightTexture, currentTexCoords).r;

        // Ray marching loop - conservative iteration count
        for (int i = 0; i < 16; i++) {
            if (currentLayerDepth >= currentDepthMapValue) {
                break; // Hit the surface
            }
            currentTexCoords -= deltaTexCoords;
            currentDepthMapValue = texture2D(u_heightTexture, currentTexCoords).r;
            currentLayerDepth += layerDepth;
        }

        // Simple interpolation
        vec2 prevTexCoords = currentTexCoords + deltaTexCoords;
        float afterDepth = currentDepthMapValue - currentLayerDepth;
        float beforeDepth = texture2D(u_heightTexture, prevTexCoords).r - currentLayerDepth + layerDepth;

        // Safe interpolation with divide-by-zero protection
        float weight = 0.5;
        if (abs(afterDepth - beforeDepth) > 0.001) {
            weight = clamp(afterDepth / (afterDepth - beforeDepth), 0.0, 1.0);
        }

        vec2 finalTexCoords = prevTexCoords * weight + currentTexCoords * (1.0 - weight);

        return finalTexCoords;
    }

    void main() {
        // Start with original texture coordinates
        vec2 texCoords = v_texCoord0;

        // Transform view direction to tangent space
        // Create TBN matrix for transforming to world space
        mat3 TBN = mat3(v_tangent, v_bitangent, v_normal);

        // Manually transpose TBN to transform from world space to tangent space
        // (transpose function is not available in all GLSL versions)
        mat3 TBN_transpose = mat3(
            v_tangent.x, v_bitangent.x, v_normal.x,
            v_tangent.y, v_bitangent.y, v_normal.y,
            v_tangent.z, v_bitangent.z, v_normal.z
        );
        vec3 tangentViewDir = normalize(TBN_transpose * v_viewDir);

        // Only apply parallax if we have a height map
        if (u_hasHeightMap > 0.5) {
            texCoords = parallaxOcclusionMapping(v_texCoord0, tangentViewDir);
            // Wrap coordinates properly
            texCoords = fract(texCoords);
        }

        // Sample diffuse texture - ALWAYS works
        vec4 texColor = texture2D(u_diffuseTexture, texCoords);

        // Sample and apply normal map
        vec3 normal = v_normal;
        vec3 normalMap = texture2D(u_normalTexture, texCoords).xyz;
        // Check if we have valid normal map data (not pure black)
        if (length(normalMap) > 0.1) {
            normalMap = normalMap * 2.0 - 1.0;
            normal = normalize(TBN * normalMap);
        }

    // Ambient lighting (very dark)
    vec3 ambient = u_ambientLight * texColor.rgb;

    // Directional moonlight (very weak)
    vec3 moonDir = normalize(u_moonDirection);
    float moonDiff = max(dot(normal, -moonDir), 0.0);
    vec3 moonlight = u_moonColor * moonDiff * texColor.rgb;

    // ===== SPOTLIGHT CALCULATION (Realistic flashlight) =====
    vec3 lightDir = normalize(u_spotPosition - v_position);
    float distance = length(u_spotPosition - v_position);

    // Calculate angle between spotlight direction and light-to-fragment direction
    vec3 spotDir = normalize(u_spotDirection);
    float theta = dot(lightDir, -spotDir);

    // Smooth transition from inner cone to outer cone (soft edges)
    float epsilon = u_spotCutoff - u_spotOuterCutoff;
    float intensity = clamp((theta - u_spotOuterCutoff) / epsilon, 0.0, 1.0);

    // Distance attenuation (quadratic falloff for realism)
    float attenuation = 1.0 / (1.0 + u_spotAttenuation * distance * distance);

    // Diffuse lighting (Lambertian) using normal from normal map
    float diff = max(dot(normal, lightDir), 0.0);

    // Specular lighting (Blinn-Phong for performance) using normal from normal map
    vec3 viewDir = normalize(u_cameraPosition - v_position);
    vec3 halfwayDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(normal, halfwayDir), 0.0), 16.0);

    // Combine spotlight components
    vec3 diffuse = u_spotColor * diff * texColor.rgb;
    vec3 specular = u_spotColor * spec * 0.3; // Subtle specular

    vec3 spotlight = (diffuse + specular) * intensity * attenuation * u_spotIntensity;

    // ===== POINT LIGHTS (Ceiling lamps) with shadows =====
    vec3 pointLighting = vec3(0.0);
    for (int i = 0; i < u_numPointLights; i++) {
        vec3 lightDir = u_pointLightPositions[i] - v_position;
        float distance = length(lightDir);
        lightDir = normalize(lightDir);

        // Shadow check - skip this light if blocked by a wall
        if (isInShadow(v_position, u_pointLightPositions[i])) {
            continue; // Light is blocked, skip this lamp
        }

        // Distance attenuation (quadratic falloff)
        float attenuation = 1.0 / (1.0 + 0.08 * distance + 0.015 * distance * distance);

        // Diffuse lighting
        float diff = max(dot(normal, lightDir), 0.0);

        // Add point light contribution
        vec3 diffuse = u_pointLightColors[i] * diff * texColor.rgb * u_pointLightIntensities[i];
        pointLighting += diffuse * attenuation;
    }

    // Combine all lighting
    vec3 litColor = ambient + moonlight + spotlight + pointLighting;

    // ===== FOG CALCULATION (Distance-based exponential fog) =====
    float distanceToCamera = length(u_cameraPosition - v_position);

    // Exponential fog with smooth falloff
    float fogFactor = 0.0;
    if (distanceToCamera > u_fogStart) {
        // Calculate fog based on distance beyond start point
        float fogDistance = distanceToCamera - u_fogStart;
        float fogRange = u_fogEnd - u_fogStart;

        // Exponential fog formula for smooth, realistic falloff
        fogFactor = 1.0 - exp(-u_fogDensity * fogDistance);
        fogFactor = clamp(fogFactor, 0.0, 1.0);

        // Make fog denser at greater distances
        float rangeRatio = clamp(fogDistance / fogRange, 0.0, 1.0);
        fogFactor = mix(fogFactor, 1.0, rangeRatio * rangeRatio);
    }

    // Mix lit color with fog color based on fog factor
    vec3 finalColor = mix(litColor, u_fogColor, fogFactor);

    // Gamma correction for more realistic appearance
    finalColor = pow(finalColor, vec3(1.0/2.2));

    gl_FragColor = vec4(finalColor, texColor.a);
}

