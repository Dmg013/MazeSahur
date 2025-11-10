// Spotlight fragment shader with realistic cone calculations and fog
    #ifdef GL_ES
    precision mediump float;
    #endif

    uniform sampler2D u_diffuseTexture;
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

    // Fog parameters
    uniform vec3 u_fogColor;          // Fog color (dark)
    uniform float u_fogDensity;       // Fog density
    uniform float u_fogStart;         // Distance where fog starts
    uniform float u_fogEnd;           // Distance where fog is maximum

    varying vec3 v_position;
    varying vec3 v_normal;
    varying vec2 v_texCoord0;

    void main() {
        // Sample texture
    vec4 texColor = texture2D(u_diffuseTexture, v_texCoord0);

    // Ambient lighting (very dark)
    vec3 ambient = u_ambientLight * texColor.rgb;

    // Directional moonlight (very weak)
    vec3 moonDir = normalize(u_moonDirection);
    float moonDiff = max(dot(v_normal, -moonDir), 0.0);
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

    // Diffuse lighting (Lambertian)
    float diff = max(dot(v_normal, lightDir), 0.0);

    // Specular lighting (Blinn-Phong for performance)
    vec3 viewDir = normalize(u_cameraPosition - v_position);
    vec3 halfwayDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(v_normal, halfwayDir), 0.0), 16.0);

    // Combine spotlight components
    vec3 diffuse = u_spotColor * diff * texColor.rgb;
    vec3 specular = u_spotColor * spec * 0.3; // Subtle specular

    vec3 spotlight = (diffuse + specular) * intensity * attenuation * u_spotIntensity;

    // Combine all lighting
    vec3 litColor = ambient + moonlight + spotlight;

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

