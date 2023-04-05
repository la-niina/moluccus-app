package moluccus.app.devops

import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay

class WideGamutEGLConfigChooser : GLSurfaceView.EGLConfigChooser {
    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
        // Define the desired EGL attributes for the configuration
        val attributes = intArrayOf(
            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, // Specify support for OpenGL ES 2.0
            EGL10.EGL_RED_SIZE, 8, // Specify 8 bits for the red color component
            EGL10.EGL_GREEN_SIZE, 8, // Specify 8 bits for the green color component
            EGL10.EGL_BLUE_SIZE, 8, // Specify 8 bits for the blue color component
            EGL10.EGL_ALPHA_SIZE, 8, // Specify 8 bits for the alpha channel
            EGL10.EGL_DEPTH_SIZE, 16, // Specify 16 bits for the depth buffer
            EGL10.EGL_SAMPLE_BUFFERS, 1, // Specify support for multisampling
            EGL10.EGL_SAMPLES, 4, // Specify 4 samples per pixel for multisampling
            EGL10.EGL_COLORSPACE,  //EGL_COLORSPACE_DISPLAY_P3_EXT, // Specify support for Display P3 color space
            EGL10.EGL_NONE // End of attribute list
        )

        // Create a list of all possible EGL configurations that match the desired attributes
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        egl.eglChooseConfig(display, attributes, configs, 1, numConfigs)

        // Return the first configuration from the list, which should be the most suitable one
        return configs[0]!!
    }

    companion object {
        private const val EGL_OPENGL_ES2_BIT = 0x0004
    }
}