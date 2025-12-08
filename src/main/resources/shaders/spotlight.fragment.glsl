// Spotlight fragment shader with parallax occlusion mapping and fog
    #ifdef GL_ES
    precision mediump float;
    #endif

    uniform sampler2D u_diffuseTexture;
    uniform sampler2D u_normalTexture;
    uniform sampler2D u_heightTexture;
    uniform sampler2D u_roughnessTexture;
    uniform sampler2D u_specularTexture;
    uniform sampler2D u_emissiveTexture;
    uniform float u_heightScale;
    uniform float u_hasHeightMap;
    uniform float u_hasRoughnessMap;
    uniform float u_hasSpecularMap;
    uniform float u_hasEmissiveMap;
    uniform vec3 u_emissiveColor;

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

    // Parallax Occlusion Mapping function - conservative version
    vec2 parallaxOcclusionMapping(vec2 texCoords, vec3 viewDir) {
        // Reduce effect based on view angle to prevent extreme distortion
        float viewDotNormal = abs(dot(vec3(0.0, 0.0, 1.0), viewDir));
        if (viewDotNormal < 0.3) {
            return texCoords; // Too shallow angle, skip parallax
        }

        // VERY conservative layer count for potato PCs
        const float minLayers = 4.0;
        const float maxLayers = 8.0;
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

        // Ray marching loop - VERY conservative for potato PCs
        for (int i = 0; i < 8; i++) {
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

        // Sample roughness map (grayscale - higher = rougher surface)
        float roughness = 0.5; // Default medium roughness
        if (u_hasRoughnessMap > 0.5) {
            roughness = texture2D(u_roughnessTexture, texCoords).r;
        }

        // Sample specular map (grayscale - higher = more reflective)
        float specularStrength = 0.3; // Default subtle specular
        if (u_hasSpecularMap > 0.5) {
            specularStrength = texture2D(u_specularTexture, texCoords).r;
        }

        // Convert roughness to shininess (rougher = lower shininess)
        // Roughness 0.0 (smooth) = shininess 128, roughness 1.0 (rough) = shininess 4
        float shininess = mix(128.0, 4.0, roughness);

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

    // Specular lighting (Blinn-Phong) using material properties
    vec3 viewDir = normalize(u_cameraPosition - v_position);
    vec3 halfwayDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(normal, halfwayDir), 0.0), shininess);

    // Combine spotlight components
    vec3 diffuse = u_spotColor * diff * texColor.rgb;
    vec3 specular = u_spotColor * spec * specularStrength;

    vec3 spotlight = (diffuse + specular) * intensity * attenuation * u_spotIntensity;

    // ===== POINT LIGHTS (Ceiling lamps) with spotlight cone effect =====
    // Shadow casting disabled for performance - lights work better without expensive raycasting
    vec3 pointLighting = vec3(0.0);
    for (int i = 0; i < u_numPointLights; i++) {
        vec3 lightDir = u_pointLightPositions[i] - v_position;
        float distance = length(lightDir);
        lightDir = normalize(lightDir);

        // Ceiling lamps shine downward in a wide cone
        vec3 lampDirection = vec3(0.0, -1.0, 0.0); // Always point down
        float lampTheta = dot(-lightDir, lampDirection);

        // Wider cone for better coverage: inner 50 degrees, outer 80 degrees
        float lampInnerCutoff = cos(radians(50.0));
        float lampOuterCutoff = cos(radians(80.0));
        float lampEpsilon = lampInnerCutoff - lampOuterCutoff;
        float lampConeIntensity = clamp((lampTheta - lampOuterCutoff) / lampEpsilon, 0.0, 1.0);

        // Skip if outside cone (optimization)
        if (lampConeIntensity < 0.01) {
            continue;
        }

        // Simplified distance attenuation - lamps reach further now
        float attenuation = 1.0 / (1.0 + 0.1 * distance + 0.02 * distance * distance);

        // Diffuse lighting
        float diff = max(dot(normal, lightDir), 0.0);

        // Specular lighting with material properties
        vec3 pointHalfwayDir = normalize(lightDir + viewDir);
        float pointSpec = pow(max(dot(normal, pointHalfwayDir), 0.0), shininess);

        // Add point light contribution with cone effect (increased intensity)
        vec3 diffuse = u_pointLightColors[i] * diff * texColor.rgb * u_pointLightIntensities[i] * 2.0;
        vec3 specular = u_pointLightColors[i] * pointSpec * specularStrength * u_pointLightIntensities[i];
        pointLighting += (diffuse + specular) * attenuation * lampConeIntensity;
    }

    // ===== EMISSIVE GLOW (Self-illumination for lamps) =====
    vec3 emissiveGlow = vec3(0.0);
    if (u_hasEmissiveMap > 0.5) {
        // Sample emissive texture
        vec3 emissiveMap = texture2D(u_emissiveTexture, texCoords).rgb;
        // Apply emissive color and make it very bright (visible even with flashlight)
        emissiveGlow = emissiveMap * u_emissiveColor * 12.0; // Strong boost for visibility
    }

    // Combine all lighting + emissive (emissive is NOT affected by lighting/shadows)
    vec3 litColor = ambient + moonlight + spotlight + pointLighting + emissiveGlow;

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

