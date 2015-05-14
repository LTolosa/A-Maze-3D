import java.util.*;

/**
 * Maze generating class.
 */

public class Maze {

    private static final byte NORTH = 0;
    private static final byte EAST  = 1;
    private static final byte WEST  = 2;
    private static final byte SOUTH = 3;

    private int[] start = {0, 0};
    private int[] end = {1, 1};

    private Cell[][] grid;

    private int rows = 1;
    private int cols = 1;

    public Maze() {
        this.grid = new Cell[1][1];
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
        this.setStart(currentCell.row, currentCell.col);

        while (visitedCells < totalCells) {
            System.out.println("First while loop! " + visitedCells + " [" + currentCell.row + ", " + currentCell.col + "] " + Arrays.toString(currentCell.walls));
            Cell[] neighbors = getNeighbors(currentCell);
            int count = 0;
            for (Cell n : neighbors){
                if(n != null)
                    count++;
            }
            boolean isEmpty = (count == 0);

            if (!isEmpty) {
                Cell chosenCell = null;
                int dir = 0;
                while (chosenCell == null) chosenCell = neighbors[dir = random.nextInt(neighbors.length)];
                breakWall(currentCell, chosenCell, dir);
                cells.push(currentCell);
                currentCell = chosenCell;
                visitedCells++;
            } else {
                currentCell = cells.pop();

            }
        }

        Cell last = currentCell;
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
            cur.solution[NORTH] = true;
            cur.walls[NORTH] = false;
            nxt.walls[SOUTH] = false;
        } else if (dir == EAST) {
            cur.solution[EAST] = true;
            cur.walls[EAST] = false;
            nxt.walls[WEST] = false;
        } else if (dir == WEST) {
            cur.solution[WEST] = true;
            cur.walls[WEST] = false;
            nxt.walls[EAST] = false;
        } else if (dir == SOUTH) {
            cur.solution[SOUTH] = true;
            cur.walls[SOUTH] = false;
            nxt.walls[NORTH] = true;
        }
    }

    public int[] getStart(){
        return this.start;
    }

    public int[] getEnd(){
        return this.end;
    }

    public Cell[][] getGrid(){
        return grid;
    }
}


