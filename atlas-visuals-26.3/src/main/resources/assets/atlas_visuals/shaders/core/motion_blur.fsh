#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;

layout(std140) uniform MotionBlurConfig {
    float Retention;
    float Seed;
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    float noise = fract(52.9829189 * fract(dot(gl_FragCoord.xy + Seed, vec2(0.06711056, 0.00583715))));
    vec3 history = texture(InSampler, texCoord).rgb + (noise - 0.5) / (255.0 * Retention);
    fragColor = vec4(history, Retention);
}
