// Spotlight vertex shader
attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec2 a_texCoord0;

uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;
uniform mat3 u_normalMatrix;

varying vec3 v_position;
varying vec3 v_normal;
varying vec2 v_texCoord0;

void main() {
    vec4 worldPos = u_worldTrans * vec4(a_position, 1.0);
    v_position = worldPos.xyz;
    v_normal = normalize(u_normalMatrix * a_normal);
    v_texCoord0 = a_texCoord0;

    gl_Position = u_projViewTrans * worldPos;
}

