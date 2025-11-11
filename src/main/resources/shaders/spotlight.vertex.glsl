// Spotlight vertex shader with tangent space for parallax occlusion mapping
attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec2 a_texCoord0;

uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;
uniform mat3 u_normalMatrix;
uniform vec3 u_cameraPosition;
uniform float u_hasHeightMap;
varying vec3 v_position;
varying vec3 v_normal;
varying vec2 v_texCoord0;
varying vec3 v_tangent;
varying vec3 v_bitangent;
varying vec3 v_viewDir;
varying vec3 v_spotDir;

void main() {
    vec4 worldPos = u_worldTrans * vec4(a_position, 1.0);
    v_position = worldPos.xyz;
    v_normal = normalize(u_normalMatrix * a_normal);

    // Use original texture coordinates
    vec2 uv = a_texCoord0;

    // Only rotate UVs for walls/floor (surfaces with height maps)
    // Enemy model has no height map, so keep original UVs
    if (u_hasHeightMap > 0.5) {
        // Rotate UVs 90 degrees clockwise for walls/floor
        vec2 temp = uv;
        uv.x = temp.y;
        uv.y = 1.0 - temp.x;
    }

    v_texCoord0 = uv;

    // Calculate tangent space based on normal orientation
    vec3 absNormal = abs(v_normal);
    vec3 tangent;
    if (absNormal.y > 0.9) {
        // Floor/ceiling
        tangent = vec3(1.0, 0.0, 0.0);
    } else if (absNormal.x > absNormal.z) {
        // X-facing wall
        tangent = vec3(0.0, 0.0, 1.0);
    } else {
        // Z-facing wall
        tangent = vec3(1.0, 0.0, 0.0);
    }

    v_tangent = normalize(u_normalMatrix * tangent);
    v_bitangent = normalize(cross(v_normal, v_tangent));

    // Calculate view direction in world space
    v_viewDir = normalize(u_cameraPosition - v_position);

    gl_Position = u_projViewTrans * worldPos;
}

