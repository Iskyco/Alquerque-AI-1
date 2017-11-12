package qirkat;

import static qirkat.PieceColor.*;
import static qirkat.Command.Type.*;

/** A Player that receives its moves from its Game's getMoveCmnd method.
 *  @author Raymond Chong
 */
class Manual extends Player {

    /** A Player that will play MYCOLOR on GAME, taking its moves from
     *  GAME. */
    Manual(Game game, PieceColor myColor) {
        super(game, myColor);
        _prompt = myColor + ": ";
    }

    @Override
    Move myMove() {
        Command cmnd = game().getMoveCmnd(_prompt);
        if (cmnd == null) {
            return null;
        }
        Move result = Move.parseMove(cmnd.operands()[0]);
        return result;
    }

    /** Identifies the player serving as a source of input commands. */
    private String _prompt;
}

