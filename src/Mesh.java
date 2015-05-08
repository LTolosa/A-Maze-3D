import static org.lwjgl.opengl.GL11.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;


public class Mesh {

    private List<Vector3f> vertices;
    private List<Vector3f> normals;
    private List<Vector2f> textures;
    private List<int[][]> faces;

    private Vector3f kA;
    private Vector3f kD;
    private Vector3f kS;

    private Vector3f pos;
    private boolean proj;

    private int modelList;

    public Mesh(String file) throws IOException {
        vertices = new ArrayList<Vector3f>();
        normals = new ArrayList<Vector3f>();
        textures = new ArrayList<Vector2f>();
        faces = new ArrayList<int[][]>();
        kA = null;
        kD = null;
        kS = null;
        loadObj(file);

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
            for(int i = 0; i < faces.size(); i++){
                int[] v = faces.get(i)[0];
                int[] n = faces.get(i)[1];
                int[] t = faces.get(i)[2];

                glTexCoord2f(textures.get(t[0]-1).x, textures.get(t[0]-1).y);
                glNormal3f(normals.get(n[0] - 1).x, normals.get(n[0] - 1).y, normals.get(n[0] - 1).z);
                glVertex3f(vertices.get(v[0] - 1).x, vertices.get(v[0] - 1).y, vertices.get(v[0] - 1).z);
                glTexCoord2f(textures.get(t[1] - 1).x, textures.get(t[1] - 1).y);
                glNormal3f(normals.get(n[1] - 1).x, normals.get(n[1] - 1).y, normals.get(n[1] - 1).z);
                glVertex3f(vertices.get(v[1] - 1).x, vertices.get(v[1] - 1).y, vertices.get(v[1] - 1).z);
                glTexCoord2f(textures.get(t[1] - 1).x, textures.get(t[1] - 1).y);
                glNormal3f(normals.get(n[2]-1).x, normals.get(n[2]-1).y, normals.get(n[2]-1).z);
                glVertex3f(vertices.get(v[2]-1).x, vertices.get(v[2]-1).y, vertices.get(v[2]-1).z);
            }
            glEnd();
        glEndList();

        return modelList;
    }

    /**
     * Loading .obj file and converts the vertexes and normals to lists to be used.
     * Taken inspiration from
     */
    public void loadObj(String file) throws IOException{
        BufferedReader reader = new BufferedReader(new FileReader(new File(file)));
        String line = "";
        String mtlName = "";
        while((line = reader.readLine()) != null){
            if(line.startsWith("v ")){
                float x = Float.valueOf(line.split(" ")[1]);
                float y = Float.valueOf(line.split(" ")[2]);
                float z = Float.valueOf(line.split(" ")[3]);
                vertices.add(new Vector3f(x, y, z));
            }
            else if(line.startsWith("vn ")){
                float x = Float.valueOf(line.split(" ")[1]);
                float y = Float.valueOf(line.split(" ")[2]);
                float z = Float.valueOf(line.split(" ")[3]);
                normals.add(new Vector3f(x, y, z));
            }
            else if(line.startsWith("vt ")){
                float s = Float.valueOf(line.split(" ")[1]);
                float t = Float.valueOf(line.split(" ")[2]);
                textures.add(new Vector2f(s, t));
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
        }

        reader.close();
    }

    public void renderMesh(){
        glTranslatef(pos.x, pos.y, pos.z);
        glCallList(modelList);
    }

}
