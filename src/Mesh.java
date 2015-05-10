import static org.lwjgl.opengl.GL11.*;

import java.io.*;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.glu.Sphere;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;
import org.newdawn.slick.util.ResourceLoader;


public class Mesh {

    private List<Vector3f> vertices;
    private List<Vector3f> normals;
    private List<Vector2f> textures;
    private List<int[][]> faces;

    //Lighting values
    private Vector3f kA;
    private Vector3f kD;
    private Vector3f kS;
    private float    nS;

    private FloatBuffer diffuse;
    private FloatBuffer ambient;
    private FloatBuffer specular;
    private FloatBuffer shininess;

    private Vector3f pos;
    private Texture texture;
    private String texName;

    private String path;

    private int modelList;

    public Mesh(String path, String file) throws IOException {
        this.path = path;

        vertices = new ArrayList<Vector3f>();
        normals = new ArrayList<Vector3f>();
        textures = new ArrayList<Vector2f>();
        faces = new ArrayList<int[][]>();
        kA = new Vector3f();
        kD = new Vector3f();
        kS = new Vector3f();
        nS = 0;
        texName = "";
        loadObj(file);
        loadLightBufs();
        loadTexture();

        pos = new Vector3f(0f, 0f, 0f);


    }

    /**
     * Creates display list for the mesh
     *
     * @return the display list
     */
    public int loadModel(){
        modelList = glGenLists(1);
        glNewList(modelList, GL_COMPILE);
            glBegin(GL_TRIANGLES);
            for(int[][] face : faces){
                int[] v = face[0];
                int[] n = face[1];
                int[] t = face[2];

                for(int i = 0; i < 3; i++) {
                    glTexCoord2f(textures.get(t[i] - 1).x, textures.get(t[i] - 1).y);
                    glNormal3f(normals.get(n[i] - 1).x, normals.get(n[i] - 1).y, normals.get(n[i] - 1).z);
                    glVertex3f(vertices.get(v[i] - 1).x, vertices.get(v[i] - 1).y, vertices.get(v[i] - 1).z);
                }
            }
            glEnd();
        glEndList();

        return modelList;
    }

    /**
     * Loading .obj file and converts the vertexes and normals to lists to be used.
     * Also loads in respective mtl file.
     *
     * Assumes each obj file has 1 model to 1 mtl file.
     * Taken inspiration from
     */
    public void loadObj(String file) throws IOException{
        BufferedReader reader = new BufferedReader(new FileReader(new File(path+file)));
        String line = "";
        String mtlFile = "";
        while((line = reader.readLine()) != null){
            if(line.startsWith("v ")){
                Vector3f tmp = new Vector3f();
                paramSet(tmp, line.split(" "));
                vertices.add(tmp);
            }
            else if(line.startsWith("vn ")){
                Vector3f tmp = new Vector3f();
                paramSet(tmp, line.split(" "));
                normals.add(tmp);
            }
            else if(line.startsWith("vt ")){
                Vector2f tmp = new Vector2f();
                paramSet(tmp, line.split(" "));
                textures.add(tmp);
            }
            else if(line.startsWith("f ")){
                Vector3f newV = new Vector3f(Float.valueOf(line.split(" ")[1].split("/")[0]),
                                             Float.valueOf(line.split(" ")[2].split("/")[0]),
                                             Float.valueOf(line.split(" ")[3].split("/")[0]));
                Vector3f newT = new Vector3f(Float.valueOf(line.split(" ")[1].split("/")[1]),
                                             Float.valueOf(line.split(" ")[2].split("/")[1]),
                                             Float.valueOf(line.split(" ")[3].split("/")[1]));
                Vector3f newN = new Vector3f(Float.valueOf(line.split(" ")[1].split("/")[2]),
                                             Float.valueOf(line.split(" ")[2].split("/")[2]),
                                             Float.valueOf(line.split(" ")[3].split("/")[2]));
                int index = faces.size();
                faces.add(new int[3][3]);
                faces.get(index)[0][0] = (int)newV.x;
                faces.get(index)[0][1] = (int)newV.y;
                faces.get(index)[0][2] = (int)newV.z;
                faces.get(index)[1][0] = (int)newN.x;
                faces.get(index)[1][1] = (int)newN.y;
                faces.get(index)[1][2] = (int)newN.z;
                faces.get(index)[2][0] = (int)newT.x;
                faces.get(index)[2][1] = (int)newT.y;
                faces.get(index)[2][2] = (int)newT.z;
            }
            else if(line.startsWith("mtllib ")){
                //Gets the name of the material file
                mtlFile = line.split(" ")[1];
            }
            else if(line.startsWith("usemtl ") && !mtlFile.isEmpty()){
                //Will only go through if material file is specified
                //Reads in material values
                String mtl = line.split(" ")[1];
                BufferedReader mtlReader = new BufferedReader(new FileReader(new File(path+mtlFile)));
                String mtlLine = "";
                boolean found = false;
                while((mtlLine=mtlReader.readLine()) != null){
                    //Will get light components if material is found
                    if(found){
                        //Reads in all the values needed
                        if(mtlLine.startsWith("Ns "))
                            nS = Float.parseFloat(mtlLine.split(" ")[1]);
                        else if(mtlLine.startsWith("Ka "))
                            paramSet(kA, mtlLine.split(" "));
                        else if(mtlLine.startsWith("Kd "))
                            paramSet(kD, mtlLine.split(" "));
                        else if(mtlLine.startsWith("Ks "))
                            paramSet(kS, mtlLine.split(" "));
                        else if(mtlLine.startsWith("map_Kd "))
                            texName = path+mtlLine.split(" ")[1];
                        else if(mtlLine.startsWith("newmtl "))
                            break;
                    }
                    else if(mtlLine.startsWith("newmtl ")){
                        if(mtlLine.split(" ")[1].equals(mtl))
                            found = true;
                    }
                }
                mtlReader.close();
            }
        }
        reader.close();
    }

    /**
     * Converts kA, kD, and kS to their respective float buffers
     */
    private void loadLightBufs(){
        ambient = BufferUtils.createFloatBuffer(4);
        diffuse = BufferUtils.createFloatBuffer(4);
        specular = BufferUtils.createFloatBuffer(4);
        shininess = BufferUtils.createFloatBuffer(4);

        //Convert vectors to float buffers
        kA.store(ambient);
        kD.store(diffuse);
        kS.store(specular);

        //Add alpha value and flip
        ambient.put(1);
        ambient.flip();
        diffuse.put(1);
        diffuse.flip();
        specular.put(1);
        specular.flip();

        //Prep shininess value
        shininess.put(nS).put(1f).put(1f).put(1f);
        shininess.flip();
    }

    private void loadTexture(){
        String ext = texName.split("\\.")[1].toUpperCase();

        try {
            texture = TextureLoader.getTexture("ext", ResourceLoader.getResourceAsStream(texName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Sets the parameter passed in by vector 3 to the latter 3
     * values in the string array passed in.
     *
     * To be used for parsing obj and mtl files.
     *
     * @param param The Vector3f to be set
     * @param vals String arrays whose latter 3 values are to be set in the Vector
     */
    private void paramSet(Vector3f param, String[] vals){
        param.set(Float.parseFloat(vals[1]),
                  Float.parseFloat(vals[2]),
                  Float.parseFloat(vals[3]));
    }

    /**
     * Sets the parameter passed in by vector 2 to the latter 2
     * values in the string array passed in.
     *
     * To be used for parsing obj and mtl files.
     *
     * @param param The Vector2f to be set
     * @param vals String arrays whose latter 2 values are to be set in the Vector
     */
    private void paramSet(Vector2f param, String[] vals){
        param.set( Float.parseFloat(vals[1]),
                   Float.parseFloat(vals[2]));
    }

    public void renderMesh(){
        //Loads in the different material attributes
        glMaterial(GL_FRONT, GL_AMBIENT, ambient);
        glMaterial(GL_FRONT, GL_DIFFUSE, diffuse);
        glMaterial(GL_FRONT, GL_SPECULAR, specular);
        glMaterial(GL_FRONT, GL_SHININESS, shininess);
        glTranslatef(pos.x, pos.y, pos.z);
        glEnable(GL_TEXTURE_2D);
        texture.bind();
        glCallList(modelList);
        glDisable(GL_TEXTURE_2D);

    }

}
