#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 vTexCoord;
uniform samplerExternalOES sampler2d;

void main() {
	gl_FragColor  = texture2D(sampler2d, vTexCoord);
}