package bright.Compiler;

import bright.Lex.Token;

public class Error {
    private int errorNo;    // 错误编号
    private String kind;    // 错误类型
    private Token token;    // 错误token
    private int line;       // 行号
    private int col;        // 列号

    public Error(int errorNo, String kind, Token token) {
        this.errorNo = errorNo;
        this.kind = kind;
        this.token = token;
        this.line = token.getLine();
        this.col = token.getCol();
    }

    public int getErrorNo() {
        return errorNo;
    }
    public String getKind() {
        return kind;
    }
    public Token getToken() {
        return token;
    }
    public int getLine() {
        return line;
    }
    public int getCol() {
        return col;
    }

    //TODO 错误形式？
    @Override
    public String toString() {
        return "Error{" +
                "errorNo=" + errorNo +
                ", kind='" + kind + '\'' +
                ", toke='" + token.getToken() + ": <" + token.getType().getTypeName() + ">" + '\'' +
                ", line=" + line + '\'' +
                ", col=" + col +
                '}';
    }
}
