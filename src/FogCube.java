import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class FogCube {

    String windowTitle = "Fog";
    public boolean closeRequested = false;

    long lastFrameTime; // used to calculate delta

    //Lights
    float[] ambient = {0.0f, 0.0f, 0.0f, 1.0f};
    float[] position = {0.0f, 0.0f, 10.0f, 1.0f};
    float[] diffuse = {0.7f, 0.7f, 0.7f, 1.0f};
    float[] specular = {0.5f, 1.0f, 1.0f, 1.0f};
    FloatBuffer ambBuf, posBuf, mDiffuseBuf, mSpecBuf;

    //Fog
    int fogMode[] = {GL_EXP, GL_EXP2, GL_LINEAR};
    int fogfilter = 0;  // Change this to see the 3 different types of fog effects!
    float fogColor[] = {0.5f, 0.5f, 0.5f, 1.0f};

    Mesh rock;

    public void run() {
        createWindow();
        getDelta(); // Initialise delta timer
        initGL();

        System.out.println("Finished loading");
        while (!closeRequested) {
            int delta = getDelta();
            pollInput(delta);
            renderGL(delta);
            lights();

            Display.update();
        }

        cleanup();
    }

    private void initGL() {

        /* OpenGL */
        int width = Display.getDisplayMode().getWidth();
        int height = Display.getDisplayMode().getHeight();

        glViewport(0, 0, width, height); // Reset The Current Viewport
        glMatrixMode(GL_PROJECTION); // Select The Projection Matrix
        glLoadIdentity(); // Reset The Projection Matrix
        GLU.gluPerspective(45.0f, ((float) width / (float) height), 0.1f, 100.0f); // Calculate The Aspect Ratio Of The Window
        glMatrixMode(GL_MODELVIEW); // Select The Modelview Matrix
        glLoadIdentity(); // Reset The Modelview Matrix

        glShadeModel(GL_SMOOTH); // Enables Smooth Shading
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);                   // Set The Blending Function For Translucency
        glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        glClearDepth(1.0f); // Depth Buffer Setup
        glEnable(GL_DEPTH_TEST); // Enables Depth Testing

        glDepthFunc(GL_LESS); // The Type Of Depth Test To Do
        glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST); // Really Nice Perspective Calculations

        ByteBuffer temp = ByteBuffer.allocateDirect(16);
        temp.order(ByteOrder.nativeOrder());

        glEnable(GL_LIGHTING);
        glEnable(GL_LIGHT0);                                      // Enable Light One
        glEnable(GL_COLOR_MATERIAL);
        glEnable(GL_NORMALIZE);
        glFogi(GL_FOG_MODE, fogMode[fogfilter]);                  // Fog Mode
        temp.asFloatBuffer().put(fogColor).flip();
        glFog(GL_FOG_COLOR, temp.asFloatBuffer());                // Set Fog Color
        glFogf(GL_FOG_DENSITY, 0.35f);                            // How Dense Will The Fog Be
        glHint(GL_FOG_HINT, GL_DONT_CARE);                   // Fog Hint Value
        glFogf(GL_FOG_START, 1.0f);                               // Fog Start Depth
        glFogf(GL_FOG_END, 5.0f);                                 // Fog End Depth
        glEnable(GL_FOG);                                         // Enables GL_FOG
        Camera.create();

        ambBuf = BufferUtils.createFloatBuffer(ambient.length);
        ambBuf.put(ambient);
        ambBuf.flip();
        posBuf = BufferUtils.createFloatBuffer(position.length);
        posBuf.put(position);
        posBuf.flip();
        mDiffuseBuf = BufferUtils.createFloatBuffer(diffuse.length);
        mDiffuseBuf.put(diffuse);
        mDiffuseBuf.flip();
        mSpecBuf = BufferUtils.createFloatBuffer(specular.length);
        mSpecBuf.put(specular);
        mSpecBuf.flip();

        try {
            rock = new Mesh("models/", "rock.obj");
            rock.loadModel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Places the lights in the scene
     */
    private void lights(){
        glPushMatrix();
        glLoadIdentity();
        Camera.apply();
        glLightModel(GL_LIGHT_MODEL_AMBIENT, ambBuf);
        glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE);
        glLight(GL_LIGHT0, GL_AMBIENT, ambBuf);
        glLight(GL_LIGHT0, GL_POSITION, posBuf);
        glLight(GL_LIGHT0, GL_DIFFUSE, mDiffuseBuf);
        glPopMatrix();
    }

    private void renderGL(int delta) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Clear The Screen And The Depth Buffer
        glLoadIdentity(); // Reset The View
        Camera.apply();

        renderRock();

        /*
        glBegin(GL_QUADS); // Start Drawing The Cube
            glColor3f(0.0f, 1.0f, 0.0f); // Set The Color To Green
            glVertex3f(1.0f, 1.0f, -1.0f); // Top Right Of The Quad (Top)
            glVertex3f(-1.0f, 1.0f, -1.0f); // Top Left Of The Quad (Top)
            glVertex3f(-1.0f, 1.0f, 1.0f); // Bottom Left Of The Quad (Top)
            glVertex3f(1.0f, 1.0f, 1.0f); // Bottom Right Of The Quad (Top)

            glColor3f(1.0f, 0.5f, 0.0f); // Set The Color To Orange
            glVertex3f(1.0f, -1.0f, 1.0f); // Top Right Of The Quad (Bottom)
            glVertex3f(-1.0f, -1.0f, 1.0f); // Top Left Of The Quad (Bottom)
            glVertex3f(-1.0f, -1.0f, -1.0f); // Bottom Left Of The Quad (Bottom)
            glVertex3f(1.0f, -1.0f, -1.0f); // Bottom Right Of The Quad (Bottom)

            glColor3f(1.0f, 0.0f, 0.0f); // Set The Color To Red
            glVertex3f(1.0f, 1.0f, 1.0f); // Top Right Of The Quad (Front)
            glVertex3f(-1.0f, 1.0f, 1.0f); // Top Left Of The Quad (Front)
            glVertex3f(-1.0f, -1.0f, 1.0f); // Bottom Left Of The Quad (Front)
            glVertex3f(1.0f, -1.0f, 1.0f); // Bottom Right Of The Quad (Front)

            glColor3f(1.0f, 1.0f, 0.0f); // Set The Color To Yellow
            glVertex3f(1.0f, -1.0f, -1.0f); // Bottom Left Of The Quad (Back)
            glVertex3f(-1.0f, -1.0f, -1.0f); // Bottom Right Of The Quad (Back)
            glVertex3f(-1.0f, 1.0f, -1.0f); // Top Right Of The Quad (Back)
            glVertex3f(1.0f, 1.0f, -1.0f); // Top Left Of The Quad (Back)

            glColor3f(0.0f, 0.0f, 1.0f); // Set The Color To Blue
            glVertex3f(-1.0f, 1.0f, 1.0f); // Top Right Of The Quad (Left)
            glVertex3f(-1.0f, 1.0f, -1.0f); // Top Left Of The Quad (Left)
            glVertex3f(-1.0f, -1.0f, -1.0f); // Bottom Left Of The Quad (Left)
            glVertex3f(-1.0f, -1.0f, 1.0f); // Bottom Right Of The Quad (Left)

            glColor3f(1.0f, 0.0f, 1.0f); // Set The Color To Violet
            glVertex3f(1.0f, 1.0f, -1.0f); // Top Right Of The Quad (Right)
            glVertex3f(1.0f, 1.0f, 1.0f); // Top Left Of The Quad (Right)
            glVertex3f(1.0f, -1.0f, 1.0f); // Bottom Left Of The Quad (Right)
            glVertex3f(1.0f, -1.0f, -1.0f); // Bottom Right Of The Quad (Right)
        glEnd(); // Done Drawing The Quad
        */
    }

    private void renderRock(){
        glPushMatrix();
            rock.renderMesh();
        glPopMatrix();
    }

    /**
     * Poll Input
     */
    public void pollInput(int delta) {
        Camera.acceptInput(delta);
        // scroll through key events
        while (Keyboard.next()) {
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_ESCAPE)
                    closeRequested = true;
            }
            if (Display.isCloseRequested()) {
                closeRequested = true;
            }
        }
    }


    /**
     * Calculate how many milliseconds have passed
     * since last frame.
     *
     * @return milliseconds passed since last frame
     */
    public int getDelta() {
        long time = (Sys.getTime() * 1000) / Sys.getTimerResolution();
        int delta = (int) (time - lastFrameTime);
        lastFrameTime = time;

        return delta;
    }

    private void createWindow() {
        try {
            Display.setDisplayMode(new DisplayMode(640, 480));
            Display.setVSyncEnabled(true);
            Display.setTitle(windowTitle);
            Display.create();
        } catch (LWJGLException e) {
            Sys.alert("Error", "Initialization failed!\n\n" + e.getMessage());
            System.exit(0);
        }
    }

    /**
     * Destroy and clean up resources
     */
    private void cleanup() {
        Display.destroy();
    }

    public static void main(String[] args) {
        new FogCube().run();
    }


}
