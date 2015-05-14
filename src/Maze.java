import org.lwjgl.util.vector.Vector3f;

import java.util.*;

/**
 * Maze generating class.
 */

public class Maze {

    static final byte NORTH = 0;
    static final byte EAST  = 1;
    static final byte WEST  = 2;
    static final byte SOUTH = 3;

    int[] start = {0, 0};
    int[] end = {1, 1};

    Cell[][] grid;
    Cell maxCell;
    Cell startCell;

    int rows;
    int cols;

    public Maze() {
        rows = 1;
        cols = 1;
        this.grid = new Cell[rows][cols];
        Arrays.fill(this.end, 1);
    }

    public Maze(int x, int y) {
        this.grid = new Cell[x][y];
        this.rows = x;
        this.cols = y;
        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.cols; c++) {
                this.grid[r][c] = new Cell(r, c);
            }
        }
        this.setBorders();
        this.end[0] = x;
        this.end[1] = y;
    }

    /* Gives all edge-cells of maze appropriate borders. */
    public void setBorders() {
        for (int n = 0; n < this.rows; n++) {
            this.grid[n][0].borders[WEST] = true;
            this.grid[n][this.cols - 1].borders[EAST] = true;
        }

        for (int m = 0; m < this.cols; m++) {
            this.grid[0][m].borders[NORTH] = true;
            this.grid[this.rows - 1][m].borders[SOUTH] = true;
        }
    }

    public void setStart(int x, int y) {
        this.start[0] = x;
        this.start[1] = y;
    }

    public void setEnd(int x, int y) {
        this.end[0] = x;
        this.end[1] = y;
    }

    /* Generates a perfect maze. */
    public void generate() {
        int totalCells = this.rows * this.cols;
        int visitedCells = 1;
        Deque<Cell> cells = new ArrayDeque<>(totalCells);
        Random random = new Random();
        Cell currentCell = this.grid[random.nextInt(this.rows)][random.nextInt(this.cols)];
        startCell = currentCell;
        this.setStart(currentCell.row, currentCell.col);
        maxCell = null;
        int depth = 1;
        int maxDepth = 0;

        while (visitedCells < totalCells) {
            random = new Random();
            Cell[] neighbors = getNeighbors(currentCell);
            int count = 0;
            for (Cell n : neighbors) count = n != null ? count + 1 : count;
            boolean isEmpty = (count == 0);

            if (!isEmpty) {
                Cell chosenCell = null;
                int dir = 0;
                while (chosenCell == null) chosenCell = neighbors[dir = random.nextInt(neighbors.length)];
                this.breakWall(currentCell, chosenCell, dir);
                cells.push(currentCell);
                depth++;
                currentCell = chosenCell;
                visitedCells++;
            } else {
                if(depth > maxDepth) {
                    maxCell = currentCell;
                    maxDepth = depth;
                }
                currentCell = cells.pop();

                depth--;
            }
        }

        Cell last = maxCell;
        this.setEnd(last.row, last.col);
    }

    public Cell[] getNeighbors(Cell cell) {
        Cell temp;
        Cell[] nbrs = new Cell[4];
        if (cell.row != 0) {
            temp = this.grid[cell.row - 1][cell.col];
            nbrs[NORTH] = temp.hasAllWalls() ? temp : null;
        }
        if (cell.col != this.cols - 1) {
            temp = this.grid[cell.row][cell.col + 1];
            nbrs[EAST] = temp.hasAllWalls() ? temp : null;
        }
        if (cell.col != 0) {
            temp = this.grid[cell.row][cell.col - 1];
            nbrs[WEST] = temp.hasAllWalls() ? temp : null;
        }
        if (cell.row != this.rows - 1) {
            temp = this.grid[cell.row + 1][cell.col];
            nbrs[SOUTH] = temp.hasAllWalls() ? temp : null;
        }
        return nbrs;
    }

    public void breakWall(Cell cur, Cell nxt, int dir) {
        if (dir == NORTH) {
            cur.walls[NORTH] = false;
            nxt.walls[SOUTH] = false;
        } else if (dir == EAST) {
            cur.walls[EAST] = false;
            nxt.walls[WEST] = false;
        } else if (dir == WEST) {
            cur.walls[WEST] = false;
            nxt.walls[EAST] = false;
        } else if (dir == SOUTH) {
            cur.walls[SOUTH] = false;
            nxt.walls[NORTH] = false;
        }
    }

    public void display(){
        for(int i = 0; i < this.cols; i++){
            Cell cur = this.grid[0][i];
            if(cur.walls[0])
                System.out.print(" _");
        }
        System.out.println();
        for(int i = 0; i < this.rows; i++){
            for(int j = 0; j < this.cols; j++){
                Cell cur = this.grid[i][j];
                if(j == 0){
                    if(cur.walls[2])
                        System.out.print("|");
                    else
                        System.out.print(" ");
                }

                if(cur.walls[3])
                    System.out.print("_");
                else
                    System.out.print(" ");
                if(cur.walls[1])
                    System.out.print("|");
                else
                    System.out.print(" ");
            }
            System.out.println();
        }

        System.out.println("Start: " + Arrays.toString(start));
        System.out.println("End: " + Arrays.toString(end));
    }

    public Cell getCurrent(Vector3f pos, float scale){
        float x = pos.x;
        float z = pos.z;

        int row = (int) (z/scale);
        int col = (int) (x/scale);

        return grid[row][col];

    }

}


