package qirkat;

import java.util.ArrayList;

import static qirkat.PieceColor.BLACK;
import static qirkat.PieceColor.WHITE;

/** A Player that computes its own moves.
 *  @author Raymond Chong
 */
class AI extends Player {

    /** Maximum minimax search depth before going to static evaluation. */
    private static final int MAX_DEPTH = 8;
    /** A position magnitude indicating a win (for white if positive, black
     *  if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 1;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new AI for GAME that will play MYCOLOR. */
    AI(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        Main.startTiming();
        Move move = findMove();
        Main.endTiming();
        Reporter reporter = game().getting();
        if (!(move == null)) {
            reporter.outcomeMsg(myColor() + " moves " + move.toString() + ".");
        }
        return move;
    }

    /** Return a move for me from the current position, assuming there
     *  is a move. */
    private Move findMove() {
        Board b = new Board(board());
        if (myColor() == WHITE) {
            findMove(b, MAX_DEPTH, true, 1, -INFTY, INFTY);
        } else {
            findMove(b, MAX_DEPTH, true, -1, -INFTY, INFTY);
        }
        return _lastFoundMove;
    }

    /** The move found by the last call to one of the ...FindMove methods
     *  below. */
    private Move _lastFoundMove;

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _lastFoundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _lastMoveFound. */
    private int findMove(Board board, int depth, boolean saveMove, int sense,
                         int alpha, int beta) {
        Move best;
        best = null;
        int bestScore;
        ArrayList<Move> moves = board.getMoves();
        if (depth == 0) {
            return staticScore(board);
        } else {
            if (board.gameOver()) {
                return staticScore(board);
            }
            if (sense == 1) {
                bestScore = -INFTY;
                for (int i = 0; i < moves.size(); i++) {
                    Move move = moves.get(i);
                    board.makeMove(move);
                    int score = findMove(new Board(board), depth - 1,
                            true, -1, alpha, beta);
                    if (score > bestScore) {
                        best = move;
                        bestScore = score;
                    }
                    alpha = Math.max(bestScore, alpha);
                    board.undo();
                    if (beta <= alpha) {
                        break;
                    }
                }
            } else {
                bestScore = INFTY;
                for (int i = 0; i < moves.size(); i++) {
                    Move move = moves.get(i);
                    board.makeMove(move);
                    int score = findMove(new Board(board), depth - 1,
                            false, 1, alpha, beta);
                    if (score < bestScore) {
                        best = move;
                        bestScore = score;
                    }
                    beta = Math.min(bestScore, beta);
                    board.undo();
                    if (beta <= alpha) {
                        break;
                    }
                }
            }
            if (saveMove) {
                assert (best != null);
                _lastFoundMove = best;
            }

            return bestScore;
        }
    }

    /** Return a heuristic value for BOARD. */
    private int staticScore(Board board) {

        int numWhite = board.piecenumber(WHITE);
        int numBlack = board.piecenumber(BLACK);
        int score = numWhite - numBlack;
        if (board.gameOver()) {
            if (numWhite > numBlack) {
                return WINNING_VALUE;
            } else {
                return -WINNING_VALUE;
            }
        }
        return score;
    }

}

