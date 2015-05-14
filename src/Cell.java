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

}
