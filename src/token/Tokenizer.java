package token;

import error.ErrorCode;
import error.TokenizeError;
import util.Format;
import vo.Pos;

public class Tokenizer {

    private StringIter it;

    public Tokenizer(StringIter it) {
        this.it = it;
    }

    /**
     * 获取下一个 Token
     * @throws TokenizeError 如果解析有异常则抛出
     */
    public Token nextToken() throws TokenizeError {
        it.readAll();

        skipSpaceCharacters();

        if (it.isEOF()) {
            return new Token(TokenType.EOF, "", it.currentPos(), it.currentPos());
        }

        char peek = it.peekChar();
        if (Character.isDigit(peek)) {
            return Number();
        } else if (Character.isAlphabetic(peek) || peek == '_') {
            return IdentOrKeyword();
        } else if (peek == '\"'){
            return StringLiteral();
        } else if (peek == '\''){
            return CharLiteral();
        }
        else {
            return OperatorOrCommentOrUnknown();
        }
    }

    private Token Number() throws TokenizeError {
        Pos startPos;
        try{
            startPos = it.currentPos();
        }catch(Error e){
            throw new TokenizeError(ErrorCode.EOF, it.currentPos());
        }
        if (it.isEOF()){
            throw new TokenizeError(ErrorCode.EOF, it.currentPos());
        }
        if (!Character.isDigit(it.peekChar())) {
            throw new TokenizeError(ErrorCode.InvalidInput, it.currentPos());
        }
        StringBuilder uint=new StringBuilder("");
        char peek = it.peekChar();
        int radix = 10;
        if(peek == '0'){
            // 不是十进制
            it.nextChar();
            peek = it.peekChar();
            if(peek == 'x' || peek == 'X'){
                radix = 16;
                it.nextChar();
                peek = it.peekChar();
                while(!it.isEOF() && (Character.isDigit(peek) || peek == 'a' || peek == 'b' || peek == 'c' || peek == 'd'
                        || peek == 'e' || peek == 'f' || peek == 'A' || peek == 'B' || peek == 'C' || peek == 'D' ||
                        peek == 'E' || peek == 'F')){
                    uint.append(it.nextChar());
                    peek = it.peekChar();
                }
            }
            else if(Character.isDigit(peek)){
                radix = 8;
                uint.append("0");
                while(!it.isEOF() && (peek == '0' || peek == '1' || peek == '2' || peek == '3' || peek == '4' ||
                        peek == '5' || peek == '6' || peek == '7')){
                    uint.append(it.nextChar());
                    peek = it.peekChar();
                }
            }
            else{
                radix = 8;
                uint.append("0");
            }
        }
        else{
            // 十进制
            while(!it.isEOF() && Character.isDigit(peek)){
                uint.append(it.nextChar());
                peek = it.peekChar();
            }
        }

        Token token;
        int num;
        try {
            num =  Integer.parseInt(uint.toString(), radix);
            token = new Token(TokenType.NUMBER_LITERAL, num, startPos, it.currentPos());
        }catch(Exception e1) {
            throw new TokenizeError(ErrorCode.InvalidNumber, it.currentPos(), "radix = " + radix + " ,number = " + uint.toString());
        }
        return token;
    }

    private Token CharLiteral() throws TokenizeError {
        Pos startPos;
        try {
            startPos = it.currentPos();
        } catch (Error e) {
            throw new TokenizeError(ErrorCode.EOF, it.currentPos());
        }
        if (it.isEOF()) {
            throw new TokenizeError(ErrorCode.EOF, it.currentPos());
        }
        if (it.peekChar() != '\'') {
            throw new TokenizeError(ErrorCode.InvalidInput, it.currentPos());
        }
        it.nextChar();
        char res;
        char peek = it.peekChar();
        if(Format.isCharRegularChar(peek)){
            res = it.nextChar();
        }
        else if(peek == '\\') {
            it.nextChar();
            peek = it.peekChar();
            switch (peek) {
                case '\'':
                    res = '\'';
                    break;
                case '\"':
                    res = '\"';
                    break;
                case '\\':
                    res = '\\';
                    break;
                case 'n':
                    res = '\n';
                    break;
                case 'r':
                    res = '\r';
                    break;
                case 't':
                    res = '\t';
                    break;
                default:
                    throw new TokenizeError(ErrorCode.InvalidInput, it.currentPos());
            }
            it.nextChar();
        }
        else{
            throw new TokenizeError(ErrorCode.InvalidChar, it.currentPos());
        }
        if(it.peekChar() != '\''){
            throw new TokenizeError(ErrorCode.InvalidChar, it.currentPos());
        }
        else{
            it.nextChar();
            return new Token(TokenType.CHAR_LITERAL, (int)  res, startPos, it.currentPos());
        }
    }

    private Token StringLiteral() throws TokenizeError {
        Pos startPos;
        try{
            startPos = it.currentPos();
        }catch(Error e){
            throw new TokenizeError(ErrorCode.EOF, it.currentPos());
        }
        if (it.isEOF()){
            throw new TokenizeError(ErrorCode.EOF, it.currentPos());
        }
        if (it.peekChar() != '\"') {
            throw new TokenizeError(ErrorCode.InvalidInput, it.currentPos());
        }
        it.nextChar();
        StringBuilder str=new StringBuilder("");
        char peek = it.peekChar();
        while(!it.isEOF() && peek != '\"'){
            if (peek == '\\'){
                it.nextChar();
                peek = it.peekChar();
                switch (peek){
                    case '\'':
                        str.append('\'');
                        break;
                    case '\"':
                        str.append('\"');
                        break;
                    case '\\':
                        str.append('\\');
                        break;
                    case 'n':
                        str.append('\n');
                        break;
                    case 'r':
                        str.append('\r');
                        break;
                    case 't':
                        str.append('\t');
                        break;
                    default:
                        throw new TokenizeError(ErrorCode.InvalidInput, it.currentPos());
                }
                it.nextChar();
            }
            else{
                str.append(it.nextChar());
            }
            peek = it.peekChar();
        }
        if(peek != '\"'){
            throw new TokenizeError(ErrorCode.InvalidString, it.currentPos());
        }
        else{
            it.nextChar();
            return new Token(TokenType.STRING_LITERAL, str.toString(), startPos, it.currentPos());
        }
    }

    private Token IdentOrKeyword() throws TokenizeError {
        StringBuilder b = new StringBuilder("");
        Pos startPos;
        try{
            startPos = it.currentPos();
        }catch(Error e){
            throw new TokenizeError(ErrorCode.EOF, it.currentPos());
        }
        if (it.isEOF()){
            throw new TokenizeError(ErrorCode.EOF, it.currentPos());
        }
        if (!Character.isLetter(it.peekChar()) && it.peekChar() != '_') {
            throw new TokenizeError(ErrorCode.InvalidInput, it.currentPos());
        }
        while (!it.isEOF() && (Character.isLetterOrDigit(it.peekChar()) || it.peekChar() == '_')) {
            b.append(it.nextChar());
        }
        String s = b.toString();
        switch (s) {
            case "const": return new Token(TokenType.CONST_KW, s, startPos, it.currentPos());
            case "while": return new Token(TokenType.WHILE_KW, s, startPos, it.currentPos());
            case "if": return new Token(TokenType.IF_KW, s, startPos, it.currentPos());
            case "else": return new Token(TokenType.ELSE_KW, s, startPos, it.currentPos());
            case "return": return new Token(TokenType.RETURN_KW, s, startPos, it.currentPos());
            case "int":
                return new Token(TokenType.INT_KW, s, startPos, it.currentPos());
            case "void":
                return new Token(TokenType.VOID_KW, s, startPos, it.currentPos());
            case "break":
                return new Token(TokenType.BREAK_KW, s, startPos, it.currentPos());
            case "continue":
                return new Token(TokenType.CONTINUE_KW, s, startPos, it.currentPos());
            default:
                return new Token(TokenType.IDENT, s, startPos, it.currentPos());
        }
    }

    private Token OperatorOrCommentOrUnknown() throws TokenizeError {
        char a = it.nextChar();
        switch (a) {
            case '+':
                int cnt = 0;
                skipSpaceCharacters();
                while(it.peekChar() == '+' || it.peekChar() == '-'){
                    if(it.peekChar() == '-'){
                        cnt++;
                    }
                    it.nextChar();
                    skipSpaceCharacters();
                }
                if(cnt % 2 == 1){
                    return new Token(TokenType.MINUS, '-', it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.PLUS, '+', it.previousPos(), it.currentPos());
            case '-':
//                if(it.peekChar() == '>'){
//                    it.nextChar();
//                    return new Token(TokenType.ARROW, "->", it.previousPos(), it.currentPos());
//                }
                cnt = 1;
                skipSpaceCharacters();
                while(it.peekChar() == '+' || it.peekChar() == '-'){
                    if(it.peekChar() == '-'){
                        cnt++;
                    }
                    it.nextChar();
                    skipSpaceCharacters();
                }
                if(cnt % 2 == 0){
                    return new Token(TokenType.PLUS, '+', it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.MINUS, '-', it.previousPos(), it.currentPos());
            case '*':
                return new Token(TokenType.MUL, a, it.previousPos(), it.currentPos());
            case '%':
                return new Token(TokenType.MOD, a, it.previousPos(), it.currentPos());
            case '/':
                if(it.peekChar() == '/'){
                    it.nextChar();
                    StringBuilder comment=new StringBuilder("");
                    char peek = it.peekChar();
                    while(!it.isEOF() && peek != '\n'){
                        comment.append(it.nextChar());
                        peek = it.peekChar();
                    }
                    return new Token(TokenType.COMMENT, "//" + comment.toString(), it.previousPos(), it.currentPos());
                }
                else if(it.peekChar() == '*'){
                    it.nextChar();
                    StringBuilder comment = new StringBuilder("");
                    char peek;
                    while(!it.isEOF()){
                        peek = it.peekChar();
                        if(peek == '*'){
                            it.nextChar();
                            peek = it.peekChar();
                            if(peek == '/'){
                                it.nextChar();
                                return new Token(TokenType.COMMENT, "/*" + comment.toString() + "*/", it.previousPos(), it.currentPos());
                            }
                            else{
                                comment.append('*');
                            }
                        }
                        else{
                            comment.append(it.nextChar());
                        }
                    }
                    throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                }
                return new Token(TokenType.DIV, a, it.previousPos(), it.currentPos());
            case '=':
                if(it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.EQ, "==", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.ASSIGN, '=', it.previousPos(), it.currentPos());
            case '!':
                if(it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.NEQ, "!=", it.previousPos(), it.currentPos());
                }
                // 连续两个！
                else if(it.peekChar() == '!'){
                    do {
                        it.nextChar();
                        if (it.peekChar() == '!') {
                            it.nextChar();
                        } else {
                            it.unreadLast();
                        }
                    }while (it.peekChar() == '!');
                    return new Token(TokenType.NEGATE, "!", it.previousPos(), it.currentPos());
                }
                else{
                    return new Token(TokenType.NEGATE, "!", it.previousPos(), it.currentPos());
                }
            case '<':
                if(it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.LE, "<=", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.LT, a, it.previousPos(), it.currentPos());
            case '>':
                if(it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.GE, ">=", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.GT, a, it.previousPos(), it.currentPos());
            case '(':
                return new Token(TokenType.L_PAREN, a, it.previousPos(), it.currentPos());
            case ')':
                return new Token(TokenType.R_PAREN, a, it.previousPos(), it.currentPos());
            case '{':
                return new Token(TokenType.L_BRACE, a, it.previousPos(), it.currentPos());
            case '}':
                return new Token(TokenType.R_BRACE, a, it.previousPos(), it.currentPos());
            case ',':
                return new Token(TokenType.COMMA, a, it.previousPos(), it.currentPos());
            case ':':
                return new Token(TokenType.COLON, a, it.previousPos(), it.currentPos());
            case ';':
                return new Token(TokenType.SEMICOLON, a, it.previousPos(), it.currentPos());
            case 0:
                return new Token(TokenType.EOF, "", it.previousPos(), it.currentPos());
            default:
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos(), String.valueOf(a));
        }
    }

    private void skipSpaceCharacters() {
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }
}
