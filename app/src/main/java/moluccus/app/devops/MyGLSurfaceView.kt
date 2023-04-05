package moluccus.app.devops

import android.content.Context
import android.opengl.GLSurfaceView

class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {
    init {
        // Set the EGL context client version to 2, indicating support for OpenGL ES 2.0
        setEGLContextClientVersion(2)

        // Set the EGL config chooser to a custom implementation that supports wide gamut color
        setEGLConfigChooser(WideGamutEGLConfigChooser())
    }
}