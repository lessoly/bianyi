package error;

import error.ErrorCode;
import vo.Pos;

public class TokenizeError extends CompileError {
    // auto-generated
    private static final long serialVersionUID = 1L;

    private ErrorCode err;
    private Pos pos;
    private String message = "";

    public TokenizeError(ErrorCode err, Pos pos) {
        super();
        this.err = err;
        this.pos = pos;
    }

    public TokenizeError(ErrorCode err, Integer row, Integer col) {
        super();
        this.err = err;
        this.pos = new Pos(row, col);
    }

    public TokenizeError(ErrorCode err, Pos pos, String message) {
        super();
        this.err = err;
        this.pos = pos;
        this.message = message;
    }

    public ErrorCode getErr() {
        return err;
    }

    public Pos getPos() {
        return pos;
    }

    @Override
    public String toString() {
        return "Tokenize Error: " + err + ", at: " + pos + ", message: " + message;
    }
}
