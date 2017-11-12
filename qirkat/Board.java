package qirkat;

import java.util.Observable;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Observer;

import static qirkat.Move.*;
import static qirkat.PieceColor.*;

/**
 * A Qirkat board.   The squares are labeled by column (a char value between
 * 'a' and 'e') and row (a char value between '1' and '5'.
 * <p>
 * For some purposes, it is useful to refer to squares using a single
 * integer, which we call its "linearized index".  This is simply the
 * number of the square in row-major order (with row 0 being the bottom row)
 * counting from 0).
 * <p>
 * Moves on this board are denoted by Moves.
 *
 * @author Raymond Chong
 */
class Board extends Observable {
    /***/
    private static final int NUM25 = 25;
    /***/
    private static final int NUM20 = 20;
    /***/
    private PieceColor[] _board = new PieceColor[NUM25];
    /***/
    private List<Move> _movesAll;
    /***/
    private Stack<Move> stack = new Stack<>();
    /***/
    private int[] horizontalMove = new int[NUM25];

    /**
     * A new, cleared board at the start of the game.
     */
    Board() {
        clear();
    }

    /**
     * A copy of B.
     */
    Board(Board b) {
        internalCopy(b);
    }

    /**
     * Return a constant view of me (allows any access method, but no
     * method that modifies it).
     */
    Board constantView() {
        return this.new ConstantBoard();
    }

    /**
     * Clear me to my starting state, with pieces in their initial
     * positions.
     */
    void clear() {
        _whoseMove = WHITE;
        _gameOver = false;
        setPieces("wwwwwwwwwwbb-wwbbbbbbbbbb", _whoseMove);
        setChanged();
        notifyObservers();
    }

    /**
     * Copy B into me.
     */
    void copy(Board b) {
        internalCopy(b);
    }

    /**
     * Copy B into me.
     */
    private void internalCopy(Board b) {
        _whoseMove = b.whoseMove();
        for (int i = 0; i < MAX_INDEX + 1; i++) {
            this.set(i, b.get(i));
            horizontalMove[i] = b.horizontalMove[i];
        }
        _gameOver = b.gameOver();
    }

    /**
     * Set my contents as defined by STR.  STR consists of 25 characters,
     * each of which is b, w, or -, optionally interspersed with whitespace.
     * These give the contents of the Board in row-major order, starting
     * with the bottom row (row 1) and left column (column a). All squares
     * are initialized to allow horizontal movement in either direction.
     * NEXTMOVE indicates whose move it is.
     */
    void setPieces(String str, PieceColor nextMove) {
        if (nextMove == EMPTY || nextMove == null) {
            throw new IllegalArgumentException("bad player color");
        }
        str = str.replaceAll("\\s", "");
        if (!str.matches("[bw-]{25}")) {
            throw new IllegalArgumentException("bad board description");
        }
        _gameOver = false;

        for (int k = 0; k < str.length(); k += 1) {
            switch (str.charAt(k)) {
            case '-':
                set(k, EMPTY);
                break;
            case 'b':
            case 'B':
                set(k, BLACK);
                break;
            case 'w':
            case 'W':
                set(k, WHITE);
                break;
            default:
                break;
            }
        }
        horizontalMove = new int[NUM25];
        stack1 = new Stack<int[]>();
        _whoseMove = nextMove;
        if (getMoves().isEmpty()) {
            _gameOver = true;
        }
        setChanged();
        notifyObservers();
    }

    /**
     * Return true iff the game is over: i.e., if the current player has
     * no moves.
     */
    boolean gameOver() {
        return _gameOver;
    }

    /**
     * Return the current contents of square C R, where 'a' <= C <= 'e',
     * and '1' <= R <= '5'.
     */
    PieceColor get(char c, char r) {
        assert validSquare(c, r);
        return get(index(c, r));
    }

    /**
     * Return the current contents of the square at linearized index K.
     */
    PieceColor get(int k) {
        assert validSquare(k);
        return _board[k];
    }

    /**
     * Set get(C, R) to V, where 'a' <= C <= 'e', and
     * '1' <= R <= '5'.
     */
    private void set(char c, char r, PieceColor v) {
        assert validSquare(c, r);
        set(index(c, r), v);
    }

    /**
     * Set get(K) to V, where K is the linearized index of a square.
     */
    private void set(int k, PieceColor v) {
        assert validSquare(k);
        _board[k] = v;
    }

    /**
     * Return true iff MOV is legal on the current board.
     */
    boolean legalMove(Move mov) {
        ArrayList<Move> checker = getMoves();
        return checker.contains(mov);
    }

    /**
     * Return a list of all legal moves from the current position.
     */
    ArrayList<Move> getMoves() {
        ArrayList<Move> result = new ArrayList<>();
        getMoves(result);
        return result;
    }

    /**
     * Add all legal moves from the current position to MOVES.
     */
    void getMoves(ArrayList<Move> moves) {
        if (gameOver()) {
            return;
        }
        if (jumpPossible()) {
            for (int k = 0; k <= MAX_INDEX; k += 1) {
                getJumps(moves, k);
            }
        } else {
            for (int k = 0; k <= MAX_INDEX; k += 1) {
                getMoves(moves, k);
            }
        }
    }

    /**
     * Add all legal non-capturing moves from the position
     * with linearized index K to MOVES.
     */
    private void getMoves(ArrayList<Move> moves, int k) {
        int c = k % 5;
        int r = k / 5;
        int colorSign;
        int lastRow;
        boolean diagonals = k % 2 == 0;
        if (_whoseMove == WHITE) {
            colorSign = 1;
            lastRow = 4;
        } else {
            colorSign = -1;
            lastRow = 0;
        }
        if (_board[k] == _whoseMove) {
            for (int i = -1; i <= 1; i++) {
                for (int j = 0; j <= 1; j++) {
                    if (validSquare((char) (c + i + 'a'), (char)
                            (r + j * colorSign + '1'))
                            && _board[index((char) (c + i + 'a'),
                            (char) (r + j * colorSign + '1'))] == EMPTY
                            && (horizontalMove[k] == 0
                            || horizontalMove[k] != -i
                            || j != 0) && (r != lastRow)
                            && (diagonals || i == 0 || j == 0)) {
                        moves.add(Move.move((char)
                                        ('a' + c), (char) ('1' + r),
                                (char) ('a' + c + i), (char)
                                        ('1' + r + j * colorSign)));
                    }
                }
            }
        }
    }

    /**
     * Add all legal captures from the position with linearized index K
     * to MOVES.
     */
    private void getJumps(ArrayList<Move> moves, int k) {
        PieceColor[] tBoard = new PieceColor[NUM25];
        for (int i = 0; i <= MAX_INDEX; i++) {
            tBoard[i] = _board[i];
        }
        moves.addAll(jumpHelper(k, tBoard));
    }

    /**
     * description sentence of function.
     *
     * @param k      parameter1 description text
     * @param tBoard parameter2 description text
     * @return return ArrayList
     */
    private ArrayList<Move> jumpHelper(int k, PieceColor[] tBoard) {
        ArrayList<Move> moves = new ArrayList<>();
        if (tBoard[k] == _whoseMove) {
            boolean diagonals = k % 2 == 0;
            for (int r = -1; r <= 1; r++) {
                for (int c = -1; c <= 1; c++) {
                    if ((row(k) + r <= '5')
                            && (row(k) + r >= '1')
                            && (col(k) + c <= 'e')
                            && (col(k) + c >= 'a')
                            && tBoard[k + r * 5 + c] == _whoseMove.opposite()) {
                        if (diagonals || r == 0 || c == 0) {
                            if ((row(k) + 2 * r <= '5')
                                    && (row(k) + 2 * r >= '1')
                                    && (col(k) + 2 * c <= 'e')
                                    && (col(k) + 2 * c >= 'a')
                                    && tBoard[k + r * 10 + c * 2] == EMPTY) {
                                PieceColor[] nBoard = new PieceColor[NUM25];
                                for (int i = 0; i < NUM25; i++) {
                                    nBoard[i] = tBoard[i];
                                }
                                nBoard[k] = EMPTY;
                                nBoard[k + r * 5 + c] = EMPTY;
                                nBoard[k + r * 10 + c * 2] = _whoseMove;
                                ArrayList<Move> tails =
                                        jumpHelper(k + r * 10 + c * 2, nBoard);
                                if (!tails.isEmpty()) {
                                    for (Move t : tails) {
                                        moves.add(Move.
                                                move(Move.move(col(k), row(k),
                                                        col(k + r * 10 + c * 2),
                                                        row(k + r * 10 + c * 2))
                                                        , t));
                                    }
                                } else {
                                    moves.add(Move.move(col(k), row(k),
                                            col(k + r * 10 + c * 2),
                                            row(k + r * 10 + c * 2)));
                                }
                            }
                        }
                    }
                }
            }
        }
        return moves;
    }

    /**
     * Return true iff MOV is a valid jump sequence on the current board.
     * MOV must be a jump or null.  If ALLOWPARTIAL, allow jumps that
     * could be continued and are valid as far as they go.
     */
    boolean checkJump(Move mov, boolean allowPartial) {
        if (mov == null) {
            return true;
        }
        if (allowPartial) {
            while (mov != null) {
                mov = mov.jumpTail();
                if (mov == null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Return true iff a jump is possible for a piece at position C R.
     */
    boolean jumpPossible(char c, char r) {
        return jumpPossible(index(c, r));
    }

    /**
     * Return true iff a jump is possible for a piece at position with
     * linearized index K.
     */
    boolean jumpPossible(int k) {
        if (_board[k] == _whoseMove) {
            boolean diagonals = k % 2 == 0;
            for (int r = -1; r <= 1; r++) {
                for (int c = -1; c <= 1; c++) {
                    if ((row(k) + r <= '5') && (row(k) + r >= '1')
                            && (col(k) + c <= 'e')
                            && (col(k) + c >= 'a')
                            && _board[k + r * 5 + c] == _whoseMove.opposite()) {
                        if (diagonals || r == 0 || c == 0) {
                            if ((row(k) + 2 * r <= '5')
                                    && (row(k) + 2 * r >= '1')
                                    && (col(k) + 2 * c <= 'e')
                                    && (col(k) + 2 * c >= 'a')
                                    && _board[k + r * 10 + c * 2] == EMPTY) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Return true iff a jump is possible from the current board.
     */
    boolean jumpPossible() {
        for (int k = 0; k <= MAX_INDEX; k += 1) {
            if (jumpPossible(k)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return the color of the player who has the next move.  The
     * value is arbitrary if gameOver().
     */
    PieceColor whoseMove() {
        return _whoseMove;
    }

    /**
     * Perform the move C0R0-C1R1, or pass if C0 is '-'.  For moves
     * other than pass, assumes that legalMove(C0, R0, C1, R1).
     *
     * @param pc sda color
     */
    void nextMove(PieceColor pc) {
        _whoseMove = pc;
    }

    /**
     * Perform the move C0R0-C1R1, or pass if C0 is '-'.  For moves
     * other than pass, assumes that legalMove(C0, R0, C1, R1).
     */
    void makeMove(char c0, char r0, char c1, char r1) {
        makeMove(Move.move(c0, r0, c1, r1, null));
    }

    /**
     * Make the multi-jump C0 R0-C1 R1..., where NEXT is C1R1....
     * Assumes the result is legal.
     */
    void makeMove(char c0, char r0, char c1, char r1, Move next) {
        makeMove(Move.move(c0, r0, c1, r1, next));
    }

    /**
     * Make the Move MOV on this Board, assuming it is legal.
     */
    void makeMove(Move mov) {
        if (legalMove(mov)) {
            boolean permit = mov.isJump();
            PieceColor thisColor = get(mov.fromIndex());
            int[] hm = new int[NUM25];
            for (int i = 0; i < NUM25; i++) {
                hm[i] = horizontalMove[i];
            }
            stack1.push(hm);
            if (!permit) {
                if (mov.row0() == mov.row1()) {
                    horizontalMove[mov.toIndex()] = mov.col1() - mov.col0();
                } else {
                    horizontalMove[mov.toIndex()] = 0;
                }
                set(mov.fromIndex(), EMPTY);
                set(mov.toIndex(), thisColor);
                stack.add(mov);
            } else {
                set(mov.fromIndex(), EMPTY);
                set(mov.jumpedIndex(), EMPTY);
                set(mov.toIndex(), thisColor);
                stack.add(mov);
            }
            horizontalMove[mov.fromIndex()] = 0;
            if (mov.jumpTail() != null) {
                mov = mov.jumpTail();
                while (mov != null) {
                    set(mov.fromIndex(), EMPTY);
                    set(mov.jumpedIndex(), EMPTY);
                    set(mov.toIndex(), thisColor);
                    mov = mov.jumpTail();
                }
                setChanged();
                notifyObservers();
            }
            _whoseMove = _whoseMove.opposite();
        } else {
            return;
        }
        if (getMoves().isEmpty()) {
            _gameOver = true;
        }
    }

    /**
     * stack the last move, if any.
     */
    private Stack<int[]> stack1 = new Stack<int[]>();

    /**
     * Undo the last move, if any.
     */
    void undo() {
        Move mov;
        if (stack.isEmpty()) {
            return;
        }
        mov = stack.pop();
        boolean allow = mov.isJump();
        PieceColor thisColor = _whoseMove.opposite();
        if (!allow) {
            set(mov.fromIndex(), thisColor);
            set(mov.toIndex(), EMPTY);
        } else {
            set(mov.fromIndex(), thisColor);
            set(mov.jumpedIndex(), thisColor.opposite());
            set(mov.toIndex(), EMPTY);
        }
        if (mov.jumpTail() != null) {
            mov = mov.jumpTail();
            while (mov != null) {
                set(mov.jumpedIndex(), thisColor.opposite());
                set(mov.toIndex(), EMPTY);
                mov = mov.jumpTail();
            }
        }
        _whoseMove = _whoseMove.opposite();
        if (!stack1.empty()) {
            horizontalMove = stack1.pop();
        }
        setChanged();
        notifyObservers();
    }


    @Override
    public boolean equals(Object o) {
        if (o instanceof Board) {
            Board b = (Board) o;
            return (b.toString().equals(toString())
                    && _whoseMove == b.whoseMove());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    /**
     * Return a text depiction of the board.  If LEGEND, supply row and
     * column numbers around the edges.
     */
    String toString(boolean legend) {
        Formatter out = new Formatter();
        for (int row = NUM20; row >= 0; row -= 5) {
            out.format("  ");
            for (int col = 0; col < 5; col += 1) {
                int k = row + col;
                if (_board[k].toString().equals("White")) {
                    out.format("w");
                }
                if (_board[k].toString().equals("Black")) {
                    out.format("b");
                }
                if (_board[k].toString().equals("Empty")) {
                    out.format("-");
                }
                if (col != 4) {
                    out.format(" ");
                }

            }
            if (row != 0) {
                out.format("\n");
            }
        }
        if (legend) {
            out.format("\na b c d e ");
        }
        return out.toString();
    }

    /**
     * description sentence of function.
     *
     * @param color parameter1 description text
     * @return return ArrayList
     */
    int piecenumber(PieceColor color) {
        int count = 0;
        for (int i = 0; i < MAX_INDEX + 1; i++) {
            if (color.equals(get(i))) {
                count++;
            }
        }
        return count;
    }

    /**
     * Return true iff there is a move for the current player.
     */
    private boolean isMove() {
        return !getMoves().isEmpty();
    }


    /**
     * Player that is on move.
     */
    private PieceColor _whoseMove;

    /**
     * Set true when game ends.
     */
    private boolean _gameOver;

    /**
     * Convenience value giving values of pieces at each ordinal position.
     */
    static final PieceColor[] PIECE_VALUES = PieceColor.values();

    /**
     * One cannot create arrays of ArrayList<Move>, so we introduce
     * a specialized private list type for this purpose.
     */
    private static class MoveList extends ArrayList<Move> {
    }

    /**
     * A read-only view of a Board.
     */
    private class ConstantBoard extends Board implements Observer {
        /**
         * A constant view of this Board.
         */
        ConstantBoard() {
            super(Board.this);
            Board.this.addObserver(this);
        }

        @Override
        void copy(Board b) {
            assert false;
        }

        @Override
        void clear() {
            assert false;
        }

        @Override
        void makeMove(Move move) {
            assert false;
        }

        /**
         * Undo the last move.
         */
        @Override
        void undo() {
            assert false;
        }

        @Override
        public void update(Observable obs, Object arg) {
            super.copy((Board) obs);
            setChanged();
            notifyObservers(arg);
        }
    }

}
