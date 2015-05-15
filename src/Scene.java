import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.openal.AL;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.openal.Audio;
import org.newdawn.slick.openal.AudioLoader;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;
import org.newdawn.slick.util.ResourceLoader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;

public class Scene {

    String windowTitle = "A-Maze-3D";
    public boolean closeRequested = false;

    long lastFrameTime; // used to calculate delta

    //Lights
    float[] ambient = {0.0f, 0.0f, 0.0f, 1.0f};
    float[] position = {0.0f, 0.3f, -5f, 1.0f};
    float[] diffuse = {0.9f, 0.8f, 0.6f, 1.0f};
    float[] specular = {0.5f, 1.0f, 1.0f, 1.0f};
    FloatBuffer ambBuf, posBuf, mDiffuseBuf, mSpecBuf;

    //Lights
    float[] ambTres = {0.0f, 0.0f, 0.0f, 1.0f};
    float[] posTres = {0.0f, 60f, 0f, 1.0f};
    float[] difTres = {0.9f, 0.9f, 0.2f, 1.0f};
    float[] specTres = {1f, 1.0f, 1.0f, 1.0f};
    float[] dirTres = {0f, -1f, 0f, 1f};
    FloatBuffer ambTresB, posTresB, difTresB, specTresB, dirTresB;

    //Fog
    int fogMode[] = {GL_EXP, GL_EXP2, GL_LINEAR};
    int fogfilter = 1;  // Change this to see the 3 different types of fog effects!
    float fogColor[] = {0.f, 0.f, 0.f, 1.0f};

    Mesh rock;
    Mesh chest;
    Mesh ghost;
    Mesh rat;
    int[] ratRows = new int[10];
    int[] ratCols = new int[10];
    Mesh skull;

    static Maze maze;

    Texture wall;
    Texture floor;

    Audio spooky_ambience;
    Audio chest_fanfare;
    Audio boo;

    int tileDL;
    int floorDL;
    int wallDL;

    static float scale = 10f;
    float t = 0f;
    float speed = .025f;

    public void run() {
        createWindow();
        getDelta(); // Initialise delta timer
        initGL();

        maze = new Maze(10, 10);
        maze.generate();
        maze.display();

        Camera.setPos(new Vector3f(maze.start[1] * 10 + 5, 3.373f, maze.start[0] * 10 + 5));
        int dir = maze.startCell.findFirstHole();

        if (dir == Maze.NORTH)
            Camera.setRotation(new Vector3f(0, 0, 0));
        else if (dir == Maze.EAST)
            Camera.setRotation(new Vector3f(0, 90, 0));
        else if (dir == Maze.WEST)
            Camera.setRotation(new Vector3f(0, -90, 0));
        else if (dir == Maze.SOUTH)
            Camera.setRotation(new Vector3f(0, 180, 0));


        loadTile();
        loadFloor();
        loadWalls();

        Random random = new Random();
        ratRows[0] = maze.start[0];
        ratCols[0] = maze.start[1];
        for (int i = 1; i < ratRows.length; i++) {
            ratRows[i] = random.nextInt(maze.rows);
            ratCols[i] = random.nextInt(maze.cols);
            System.out.println("Rat at: " + ratCols[i] + ", " + ratRows[i]);
        }

        while (!closeRequested) {
            int delta = getDelta();
            pollInput(delta);
            renderGL(delta);
            lights();
            Display.update();

            // if chest is reached
            if (maze.getCurrent(Camera.getPos(), scale).equals(maze.maxCell)) {

                spooky_ambience.stop();
                chest_fanfare.playAsSoundEffect(1.0f, 1.0f, false);

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                closeRequested = true;
            }
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
        GLU.gluPerspective(45.0f, ((float) width / (float) height), 0.0999999f, 500.0f); // Calculate The Aspect Ratio Of The Window
        glMatrixMode(GL_MODELVIEW); // Select The Modelview Matrix
        glLoadIdentity(); // Reset The Modelview Matrix

        glShadeModel(GL_SMOOTH); // Enables Smooth Shading
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);                   // Set The Blending Function For Translucency
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClearDepth(1.0f); // Depth Buffer Setup
        glEnable(GL_DEPTH_TEST); // Enables Depth Testing

        glDepthFunc(GL_LESS); // The Type Of Depth Test To Do
        glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST); // Really Nice Perspective Calculations

        ByteBuffer temp = ByteBuffer.allocateDirect(16);
        temp.order(ByteOrder.nativeOrder());

        glEnable(GL_LIGHTING);
        glEnable(GL_LIGHT0);                                      // Enable Light One
        glEnable(GL_LIGHT1);
        glEnable(GL_COLOR_MATERIAL);
        glEnable(GL_NORMALIZE);
        glFogi(GL_FOG_MODE, fogMode[fogfilter]);                  // Fog Mode
        temp.asFloatBuffer().put(fogColor).flip();
        glFog(GL_FOG_COLOR, temp.asFloatBuffer());                // Set Fog Color
        glFogf(GL_FOG_DENSITY, 0.05f);                            // How Dense Will The Fog Be
        glHint(GL_FOG_HINT, GL_DONT_CARE);                   // Fog Hint Value
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

        ambTresB = BufferUtils.createFloatBuffer(ambTres.length);
        ambTresB.put(ambTres);
        ambTresB.flip();
        posTresB = BufferUtils.createFloatBuffer(posTres.length);
        posTresB.put(posTres);
        posTresB.flip();
        difTresB = BufferUtils.createFloatBuffer(difTres.length);
        difTresB.put(difTres);
        difTresB.flip();
        specTresB = BufferUtils.createFloatBuffer(specTres.length);
        specTresB.put(specTres);
        specTresB.flip();
        dirTresB = BufferUtils.createFloatBuffer(dirTres.length);
        dirTresB.put(dirTres);
        dirTresB.flip();

        try {
            rock = new Mesh("models/", "rock.obj");
            rock.loadModel();

            chest = new Mesh("models/", "treasure_chest.obj");
            chest.loadModel();

            ghost = new Mesh("models/", "Creature.obj");
            ghost.loadModel();

            skull = new Mesh("models/", "Skull.obj");
            skull.loadModel();

            rat = new Mesh("models/", "rat_01.obj");
            rat.loadModel();

            wall = TextureLoader.getTexture("JPG", ResourceLoader.getResourceAsStream("models/dungeon_walls_1.jpg"));
            floor = TextureLoader.getTexture("JPG", ResourceLoader.getResourceAsStream("models/dungeon__floor.jpg"));

            spooky_ambience = AudioLoader.getAudio("WAV", ResourceLoader.getResourceAsStream("sounds/spooky_ambience.wav"));
            spooky_ambience.playAsSoundEffect(1.0f, 1.0f, true);

            chest_fanfare = AudioLoader.getAudio("WAV", ResourceLoader.getResourceAsStream("sounds/chest_fanfare.wav"));
            boo = AudioLoader.getAudio("WAV", ResourceLoader.getResourceAsStream("sounds/boo_laugh.wav"));

            // polling is required to allow streaming to get a chance to
            // queue buffers
            //SoundStore.get().poll(0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Places the lights in the scene
     */
    private void lightsChest(float x, float z) {

        if (Keyboard.isKeyDown(Keyboard.KEY_UP))
            posTres[2] += 0.1;
        if (Keyboard.isKeyDown(Keyboard.KEY_DOWN))
            posTres[2] -= 0.1;
        if (Keyboard.isKeyDown(Keyboard.KEY_LEFT))
            posTres[0] += 0.1;
        if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT))
            posTres[0] -= 0.1;
        if (Keyboard.isKeyDown(Keyboard.KEY_RETURN))
            posTres[1] += 0.1;
        if (Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
            posTres[1] -= 0.1;

        posTres[0] = x;
        posTres[2] = z;

        posTresB = BufferUtils.createFloatBuffer(posTres.length);
        posTresB.put(posTres);
        posTresB.flip();
        glPushMatrix();
        glLoadIdentity();
        Camera.apply();
        glLightModel(GL_LIGHT_MODEL_AMBIENT, ambBuf);
        glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE);
        glLight(GL_LIGHT1, GL_AMBIENT, ambTresB);
        glLight(GL_LIGHT1, GL_POSITION, posTresB);
        glLight(GL_LIGHT1, GL_DIFFUSE, difTresB);
        glLight(GL_LIGHT1, GL_SPOT_DIRECTION, dirTresB);
        glLighti(GL_LIGHT1, GL_SPOT_CUTOFF, 15);
        glLighti(GL_LIGHT1, GL_SPOT_EXPONENT, 78);
        glPopMatrix();
    }

    private void lights() {
        /*
        if (Keyboard.isKeyDown(Keyboard.KEY_UP))
            position[2] += 0.1;
        if (Keyboard.isKeyDown(Keyboard.KEY_DOWN))
            position[2] -= 0.1;
        if (Keyboard.isKeyDown(Keyboard.KEY_LEFT))
            position[0] += 0.1;
        if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT))
            position[0] -= 0.1;
        if(Keyboard.isKeyDown(Keyboard.KEY_RETURN))
            position[1] += 0.1;
        if(Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
            position[1] -=0.1;
        */

        posBuf = BufferUtils.createFloatBuffer(position.length);
        posBuf.put(position);
        posBuf.flip();
        glPushMatrix();
        glLoadIdentity();
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
        renderFloor();
        renderWalls();
        renderChest();
        //renderGhost();

        t += speed;

        for (int i = 0; i < ratRows.length; i++) {
            glPushMatrix();
            glTranslatef(scale * ratCols[i] + 5 + 3.5f * (float) Math.cos(t), 0, scale * ratRows[i] + 5 + 3.5f * (float) Math.sin(t));
            glRotatef((float) -Math.toDegrees(t), 0, 1, 0);
            glScalef(4f, 4f, 4f);
            renderRat();
            glPopMatrix();
        }

        // map of maze
        glDisable(GL_LIGHTING);
        glPushMatrix();
        glLoadIdentity();

        Cell currCell = maze.getCurrent(Camera.getPos(), scale);
        int x = currCell.col;
        int y = currCell.row;

        maze.setVisibleCells(Camera.getPos(), scale);

        Cell endCell = maze.maxCell;
        int endX = endCell.col;
        int endY = endCell.row;

        float lineSize = .002f;
        glColor3f(1f, 1f, 1f);

        for (int i = 0; i < maze.rows; i++) {
            for (int j = 0; j < maze.cols; j++) {
                Cell curr = maze.grid[i][j];
                if (curr.visible) {
                    if (curr.walls[0]) {
                        glBegin(GL_LINES);
                        glVertex3f(.045f + lineSize * j, .035f - lineSize * i, -0.1f);
                        glVertex3f(.045f + lineSize * (j + 1), .035f - lineSize * i, -0.1f);
                        glEnd();
                    }

                    if (curr.walls[3]) {
                        glBegin(GL_LINES);
                        glVertex3f(.045f + lineSize * j, .035f - lineSize * (i + 1), -0.1f);
                        glVertex3f(.045f + lineSize * (j + 1), .035f - lineSize * (i + 1), -0.1f);
                        glEnd();
                    }

                    if (curr.walls[2]) {
                        glBegin(GL_LINES);
                        glVertex3f(.045f + lineSize * j, .035f - lineSize * i, -0.1f);
                        glVertex3f(.045f + lineSize * j, .035f - lineSize * (i + 1), -0.1f);
                        glEnd();
                    }

                    if (curr.walls[1]) {
                        glBegin(GL_LINES);
                        glVertex3f(.045f + lineSize * (j + 1), .035f - lineSize * i, -0.1f);
                        glVertex3f(.045f + lineSize * (j + 1), .035f - lineSize * (i + 1), -0.1f);
                        glEnd();
                    }
                }
            }
        }

        glBegin(GL_QUADS);
        glColor3f(1f, 0f, 0f);
        // Your position
        glVertex3f(.045f + lineSize * x, .035f - lineSize * y, -0.1f);
        glVertex3f(.045f + lineSize * x, .035f - lineSize * (y + 1), -0.1f);
        glVertex3f(.045f + lineSize * (x + 1), .035f - lineSize * (y + 1), -0.1f);
        glVertex3f(.045f + lineSize * (x + 1), .035f - lineSize * y, -0.1f);

        //Chest Position
        glColor3f(1f, 1f, 0f);
        glVertex3f(.045f + lineSize * endX, .035f - lineSize * endY, -0.1f);
        glVertex3f(.045f + lineSize * endX, .035f - lineSize * (endY + 1), -0.1f);
        glVertex3f(.045f + lineSize * (endX + 1), .035f - lineSize * (endY + 1), -0.1f);
        glVertex3f(.045f + lineSize * (endX + 1), .035f - lineSize * endY, -0.1f);

        glEnd();

        glPopMatrix();
        glEnable(GL_LIGHTING);

    }

    private void loadTile() {
        tileDL = glGenLists(1);
        glNewList(tileDL, GL_COMPILE);
        glBegin(GL_QUADS);
        glTexCoord2f(0f, 1f);
        glVertex3f(0.0f, 0.0f, 1.0f); // Bottom Left Of The Quad (Back)
        glTexCoord2f(1f, 1f);
        glVertex3f(1.0f, 0.0f, 1.0f); // Bottom Right Of The Quad (Back)
        glTexCoord2f(1f, 0f);
        glVertex3f(1.0f, 0.0f, 0.0f); // Top Right Of The Quad (Back)
        glTexCoord2f(0f, 0f);
        glVertex3f(0.0f, 0.0f, 0.0f); // Top Left Of The Quad (Back)
        glEnd();
        glEndList();
    }


    private void loadFloor() {
        floorDL = glGenLists(1);
        glNewList(floorDL, GL_COMPILE);
        for (int i = 0; i < maze.rows; i++) {
            for (int j = 0; j < maze.cols; j++) {
                glPushMatrix();
                glTranslatef(i, 0, j);
                glCallList(tileDL);
                glPopMatrix();
            }
        }
        glEndList();
    }

    private void renderFloor() {
        glPushMatrix();
        glTranslatef(0f, 0f, 0f);
        glScalef(scale, scale, scale);
        glColor3f(1f, 1f, 1f);
        glEnable(GL_TEXTURE);
        floor.bind();
        glCallList(floorDL);
        glDisable(GL_TEXTURE);
        glPopMatrix();
        /*
        glBegin(GL_LINES);
        glColor3f(0f, 0f, 0f);
        glVertex3f(.034f, .0154f, -0.105f);
        glVertex3f(.036f, .0154f, -0.105f);
        glEnd();
        */

    }

    private void loadWalls() {
        float angle = -90f;
        wallDL = glGenLists(1);
        glNewList(wallDL, GL_COMPILE);
        for (int i = 0; i < maze.cols; i++) {
            Cell cur = maze.grid[0][i];
            if (cur.walls[0]) {
                glPushMatrix();
                glTranslatef(i, 0, 0);
                glRotatef(angle, 1, 0, 0);
                glCallList(tileDL);
                glPopMatrix();
            }
        }
        for (int i = 0; i < maze.rows; i++) {
            for (int j = 0; j < maze.cols; j++) {
                Cell cur = maze.grid[i][j];
                if (j == 0) {
                    if (cur.walls[2]) {
                        glPushMatrix();
                        glTranslatef(0f, 0f, i);
                        glRotatef(-angle, 0, 0, 1);
                        glCallList(tileDL);
                        glPopMatrix();

                    }
                }

                if (cur.walls[3]) {
                    glPushMatrix();
                    glTranslatef(j, 0, i + 1);
                    glRotatef(angle, 1, 0, 0);
                    glCallList(tileDL);
                    glPopMatrix();
                }
                if (cur.walls[1]) {
                    glPushMatrix();
                    glTranslatef(j + 1, 0, i);
                    glRotatef(-angle, 0, 0, 1);
                    glCallList(tileDL);
                    glPopMatrix();
                }
            }
        }
        glEndList();
    }

    private void renderWalls() {
        glPushMatrix();
        glTranslatef(0f, 0f, 0f);
        glScalef(scale, scale, scale);
        glColor3f(1f, 1f, 1f);
        glEnable(GL_TEXTURE);
        wall.bind();
        glCallList(wallDL);
        glDisable(GL_TEXTURE);
        glPopMatrix();
    }

    private void renderRock() {
        glPushMatrix();
        //glScalef(10f, 10f, 10f);
        rock.renderMesh();
        glPopMatrix();
    }

    private void renderRat() {
        glPushMatrix();
        rat.renderMesh();
        glPopMatrix();
    }

    private void renderChest() {
        int dir = maze.maxCell.findFirstHole();
        glPushMatrix();
        glTranslatef(maze.end[1] * 10 + 5, 0f, maze.end[0] * 10 + 5);
        if (dir == Maze.NORTH)
            glRotatef(180f, 0, 1, 0);
        else if (dir == Maze.EAST)
            glRotatef(90f, 0, 1, 0);
        else if (dir == Maze.WEST)
            glRotatef(-90f, 0, 1, 0);
        glScalef(2f, 2f, 2f);
        chest.renderMesh();
        glPopMatrix();
        float x = maze.end[1] * 10 + 5;
        float z = maze.end[0] * 10 + 5;

        if (dir == Maze.NORTH)
            z += 3;
        else if (dir == Maze.EAST)
            x += 3;
        else if (dir == Maze.WEST)
            x -= 3;
        else if (dir == Maze.SOUTH)
            z -= 3;
        lightsChest(x, z);

        glPushMatrix();
            glTranslatef(x, 0f, z);
            glScalef(3f, 3f, 3f);
            skull.renderMesh();
        glPopMatrix();
    }

    private void renderGhost() {
        glPushMatrix();
        glTranslatef(0f, 0f, 10f);
        //glScalef(.1f, .1f, .1f);
        ghost.renderMesh();
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
                AL.destroy();
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
            Display.setDisplayMode(new DisplayMode(960, 540));
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
        new Scene().run();
    }


}
