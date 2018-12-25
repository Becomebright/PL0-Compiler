package bright.Compiler;


import java.util.ArrayList;
import java.util.List;

/**
 * 符号类型
 */
enum SymbolKind {
    constant, variable, procedure
}

/**
 * 符号
 */
class Symbol{
    private String name;        // 名称
    private SymbolKind kind;    // 常量 / 变量 / 过程
    private int val;            // 常量或变量的值
    private int level;          // 嵌套层次
    private int adr;            // 相对与所在嵌套过程基地址的偏移地址

    // 常量的构造函数
    public Symbol(String name, SymbolKind kind, int val, int level, int adr) {
        this.name = name;
        this.kind = kind;
        this.val = val;
        this.level = level;
        this.adr = adr;
    }
    // 变量或过程的构造函数（声明时没有没有值）
    public Symbol(String name, SymbolKind kind, int level, int adr) {
        this.name = name;
        this.kind = kind;
        this.level = level;
        this.adr = adr;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public SymbolKind getKind() {
        return kind;
    }
    public void setKind(SymbolKind kind) {
        this.kind = kind;
    }
    public int getVal() {
        return val;
    }
    public void setVal(int val) {
        this.val = val;
    }
    public int getLevel() {
        return level;
    }
    public void setLevel(int level) {
        this.level = level;
    }
    public int getAdr() {
        return adr;
    }
    public void setAdr(int adr) {
        this.adr = adr;
    }
}

/**
 * 符号表
 */
public class SymbolTable {
    public List<Symbol> symbolTable;
    private int ptr = 0;

    public SymbolTable() {
        this.symbolTable = new ArrayList<>();
    }

    // 向符号表中插入常量
    public void putConst(String name, int val, int level, int adr){
        symbolTable.add(new Symbol(name, SymbolKind.constant,
                val, level, adr));
        ptr++;
    }
    // 向符号表中插入变量
    public void putVar(String name, int level, int adr){
        symbolTable.add(new Symbol(name, SymbolKind.variable,
                level, adr));
        ptr++;
    }
    // 向符号表中插入过程
    public void putProc(String name, int level, int adr){
        symbolTable.add(new Symbol(name, SymbolKind.procedure,
                level, adr));
        ptr++;
    }

    // 当前层是否有某符号
    // 线性查找
    public boolean inThisLevel(String name, int level){
        for(int i=symbolTable.size()-1; i>=0; i--){
            Symbol symbol = symbolTable.get(i);
            if(symbol.getLevel()==level && symbol.getName().equals(name))
                return true;
        }
        return false;
    }

    // 判断标识符是否被定义过，即当前层及之前层是否有某符号
    // 线性查找
    public boolean isDefined(String name, int level){
        for(int i=symbolTable.size()-1; i>=0; i--){
            Symbol symbol = symbolTable.get(i);
            if(symbol.getLevel()<=level && symbol.getName().equals(name))
                return true;
        }
        return false;
    }

    // 按名称查找变量
    public Symbol getSymbol(String name){
        for(int i=symbolTable.size()-1; i>=0; i--)
            if(symbolTable.get(i).getName().equals(name))
                return symbolTable.get(i);
        return null;
    }
    // 查找当前层所在的过程
//    public int getProc(int level){
//
//    }

    public List<Symbol> getSymbolTable() {
        return symbolTable;
    }

    public int getPtr() {
        return ptr;
    }

    /**
     * @param level 层的标号
     * @return level层在符号表中的开始位置，没找到则返回-1
     */
    public int getInitPos(int level){
        for(int i=symbolTable.size()-1; i>=0; i--){
            if(symbolTable.get(i).getKind() == SymbolKind.procedure)
                return i;
        }
        return -1;
    }
}
