package bright.Lex;

import bright.Compiler.Error;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//TODO: 错误处理

public class LexAnalyzer {
    private String[] keywords = {
            "const", "var", "procedure", "odd", "if", "then", "else", "while",
            "call", "begin", "end", "repeat", "until", "read", "write", "do"
    };
    private char ch = ' ';  // 当前读进的字符
    private String token;   // 当前分析的字符串
    private int ptr = 0;    // 指向当前字符的指针
    private int line = 1;   // 当前行
    private int col = 0;    // 当前列
    private char[] buffer;  // 输入的源代码
    private List<Token> tokenLists;  // 识别出的所有token
    private List<Error> errors;

    public LexAnalyzer(File file) {
        tokenLists = new ArrayList<>();
        errors = new ArrayList<>();
        BufferedReader bf;
        try {
            bf = new BufferedReader(new FileReader(file));
            String s1;
            StringBuilder s2 = new StringBuilder();
            while((s1 = bf.readLine()) != null)
                s2.append(s1).append(String.valueOf('\n'));
            buffer = s2.toString().toCharArray();
            bf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        analyze();
    }

    public List<Token> getTokens() {
        return tokenLists;
    }

    private void analyze(){
        while(ptr < buffer.length){
            tokenLists.add(getSym());
        }
    }

    public Token getSym(){  // 读token
        clearToken();
        getChar();
        while(ch == ' ' || ch == '\n' || ch == '\r' || ch == '\0' || ch == '\t'){
            if (ch == '\n'){
                line++;
                col = 0;
            }
            getChar();
        }
        if(ch == '$')
            return new Token(token, SymType.EOF, "-", line, col);
        if(isLetter()){  // 保留字 or 标识符
            while(isLetter() || isDigit()){
                catToken();
                getChar();
            }
            retract();
            int resultValue = reserve();
            if(resultValue == -1)  // 不是保留字，是标识符
                return new Token(token, SymType.SYM, token, line, col);
            else return new Token(token, SymType.values()[resultValue], "-", line, col);
        }
        else if(isDigit()){  // 数值
            while(isDigit()){
                catToken();
                getChar();
            }
            if(ch == '.'){
                catToken();
                getChar();
                if(isDigit()){  // 小数
                    while(isDigit()){
                        catToken();
                        getChar();
                    }
                    retract();
                    return new Token(token, SymType.REAL, transNum(1), line, col);
                }
            }
            else{
                retract();
                return new Token(token, SymType.INT, transNum(0), line, col);
            }
        }
        else if(ch == '=')    // =
            return new Token("=", SymType.EQU, "-", line, col); // '='
        else if(ch == '+')   // +
            return new Token("+", SymType.ADD, "-", line, col); // '+'
        else if(ch == '-')  // -
            return new Token("-", SymType.SUB, "-", line, col); // '-'
        else if(ch == '*')   // *
            return new Token("*", SymType.MUL, "-", line, col); // '*'
        else if(ch == '/'){  // / or 注释
            getChar();
            if(ch == '*'){
                do{
                    do{ getChar(); } while(ch != '*');
                    do{ getChar(); if(ch == '/') return null; } while(ch == '*');
                }while(ch != '*');
            }
            retract();
            return new Token("/", SymType.DIV, "-", line, col);  // '/'
        }
        else if(ch == '<') {  // < or <= or <>
            getChar();
            if(ch == '=')
                return new Token("<=", SymType.LESE, "-", line, col);  // '<='
            else if(ch == '>')
                return new Token("<>", SymType.NEQE, "-", line, col);  // '<>'
            else{
                retract();
                return new Token("<", SymType.LES, "-", line, col);  // '<'
            }
        }
        else if(ch == '>') {  // '>' or '>='
            getChar();
            if(ch == '=')
                return new Token(">=", SymType.LARE, "-", line, col);  // '>='
            else{
                retract();
                return new Token(">", SymType.LAR, "-", line, col);  // '>'
            }
        }
        else if(ch == '(')   // (
            return new Token("(", SymType.LBR, "-", line, col);    // '('
        else if(ch == ')')   // )
            return new Token(")", SymType.RBR, "-", line, col);    // ')'
        else if(ch == ',')   // ,
            return new Token(",", SymType.COMMA, "-", line, col);  // ','
        else if(ch == ';')   // ;
            return new Token(";", SymType.SEMIC, "-", line, col);  // ';'
        else if(ch == '.')   // .
            return new Token(".", SymType.POI, "-", line, col);    // '.'
        else if(ch == ':'){  // ':=' or ':'
            getChar();
            if(ch == '=')
                return new Token(":=", SymType.CEQU, "-", line, col); // ':='
            else{
                retract();
                return new Token("=", SymType.COL, "-", line, col);  // '='
            }
        }
        else {  // 错误处理
            // errors.add(new Error(57, "非法符号", "", line));
            return new Token(token, SymType.ERROR, "非法符号", line, col);
        }
        return new Token(token, SymType.EOF, "-", line, col);
    }

    private void getChar() {
        col++;
        if(ptr < buffer.length)
            ch =  buffer[ptr++];
        else
            ch = '$';
    }
    private void clearToken() { token = ""; }
    private boolean isLetter(){
        return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z');
    }
    private boolean isDigit(){ return ch >= '0' && ch <='9'; }
    private void catToken(){ token += ch; }
    private void retract(){
        ch = buffer[--ptr];
        col--;
    }
    private int reserve(){  // 判断token是否为保留字，若是则返回保留字编码，否则返回-1
        for(int i=0; i<keywords.length; i++){
            if(token.equals(keywords[i]))
                return i;
        }
        return -1;
    }
    private String transNum(int type){  // 返回数值的二进制表示，省略开始的0
        if(type == 0){  // 整数
            return toBinaryString(Integer.parseInt(token)) + "(二进制)";
        }else{  // 小数
            return toBinaryString(Double.parseDouble(token)) + "(二进制)";
        }
    }

    /* 十进制整数转二进制 */
    private String toBinaryString(int number) {
        if(number == 0)
            return "0";
        StringBuilder sBuilder = new StringBuilder();
        for (int i = 0; i < 32; i++){
            sBuilder.append(number & 1);
            number = number >>> 1;
        }
        int index = sBuilder.reverse().indexOf("1");
        return sBuilder.substring(index);
    }

    /* 十进制小数转二进制（10位小数） */
    private String toBinaryString(double number) {
        int in = (int)number;   // 取整数部分
        double d = number - in; // 取小数部分
        StringBuilder res = new StringBuilder();

        res.append(toBinaryString(in));  // 转换整数部分
        res.append('.');    // 小数点
        //转换整数部分
        int cnt = 10;
        double multi;
        while(cnt >= 0){
            multi = d * 2;
            if(multi >= 1){
                res.append(1);
                d = multi - 1;
            }else{
                res.append(0);
                d = multi;
            }
            cnt--;
        }
        return String.valueOf(res);
    }
}
