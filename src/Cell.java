import org.lwjgl.util.vector.Vector3f;

/**
 * Defines cell in a maze.
 */
public class Cell {
    /* Cell parameters. Each array element indicates a
     * condition's status in a cardinal direction. The
     * order is N, E, W, S.
     */
    public boolean[] walls     = {true, true, true, true};
    public boolean[] borders    = {false, false, false, false};

    public int row;
    public int col;

    public Cell(int x, int y) {
        this.row = x;
        this.col = y;
    }

    public boolean hasAllWalls() {
        return walls[0] && walls[1] && walls[2] && walls[3];
    }

    public int findFirstHole(){
        for(int i = 0; i < this.walls.length; i++){
            if(!this.walls[i])
                return i;
        }
        return 0;
    }

    public void collisionCheck(Vector3f pos, Vector3f prev, float scale){
        float centerX = col*scale + scale/2f;
        float centerZ = row*scale + scale/2f;
        if(walls[0])
            pos.z = Math.max(centerZ - scale/2f + 0.5f, pos.z);
        if(walls[1])
            pos.x = Math.min(centerX + scale/2f - 0.5f, pos.x);
        if(walls[2])
            pos.x = Math.max(centerX - scale/2f + 0.5f, pos.x);
        if(walls[3])
            pos.z = Math.min(centerZ + scale/2f - 0.5f, pos.z);
    }

}
