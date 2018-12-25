package bright.Lex;

public class Token {
    // 对于整数和小数，token中存数值(如123.23)，value中存二进制形式
    // 对于其它token，value为"-"，没有用
    private String token;   // 代码中单词
    private SymType type;   // 类型，一符一类
    private String value;   // 值
    private int line;
    private int col;

    Token(String token, SymType type, String value, int line, int col) {
        this.token = token;
        this.type = type;
        this.value = value;
        this.line = line;
        this.col = col;
    }

    public String getToken() {
        return token;
    }
    public SymType getType() {
        return type;
    }
    public String getValue() {
        return value;
    }
    public int getLine() {
        return line;
    }
    public int getCol() {
        return col;
    }
}
