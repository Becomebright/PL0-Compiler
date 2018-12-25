package bright.Compiler;

import bright.Lex.LexAnalyzer;
import bright.Lex.SymType;
import bright.Lex.Token;
import javafx.scene.control.SeparatorMenuItem;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//TODO 偏移地址的计算可能有问题，和MyCompiler生成的P-Code不一样
/**
 * 递归下降分析器 Recursive Descent Analyzer
 */
public class RD_Analyzer {
    private List<Token> tokens;         // 词法分析结果
    private int ptr = 0;                // 指向当前token
    private Token token;                // 当前读入的token

    private SymbolTable symbolTable;    // 符号表
    private PcodeList pcodeList;        // P-code
    private List<Error> errors;         // 错误信息

    private int level;                  // 当前嵌套层次
    private int address;                // 当前偏移地址
    private int addressIncrement = 1;

    private Set<SymType> declarationHead;
    private Set<SymType> factorHead;
    private Set<SymType> statementHead;


    public RD_Analyzer(File file) {
        errors = new ArrayList<>();
        LexAnalyzer lex = new LexAnalyzer(file);
        tokens = lex.getTokens();
        symbolTable = new SymbolTable();
        pcodeList = new PcodeList();

        initSymTypeSet();
    }

    private void initSymTypeSet() {
        declarationHead = new HashSet<>();
        declarationHead.add(SymType.CON);
        declarationHead.add(SymType.VAR);
        declarationHead.add(SymType.PROC);

        factorHead = new HashSet<>();
        factorHead.add(SymType.SYM);
        factorHead.add(SymType.INT);

        statementHead = new HashSet<>();
        statementHead.add(SymType.IF);
        statementHead.add(SymType.WHILE);
        statementHead.add(SymType.CALL);
        statementHead.add(SymType.READ);
        statementHead.add(SymType.WRITE);
        statementHead.add(SymType.BEGIN);
        statementHead.add(SymType.SYM);
        statementHead.add(SymType.REP);
    }

    // 编译, 没有错误则返回true
    public boolean compile(){
        nextSym();
        program();
        return errors.size() == 0;
    }

    // <主程序>::=<分程序>.
    private void program() {
        Set<SymType> symTypes = new HashSet<>();
        symTypes.addAll(declarationHead);
        symTypes.addAll(statementHead);
        symTypes.add(SymType.POI);
        block(symTypes);
        if(token.getType() == SymType.POI){
            nextSym();
            if(token.getType() != SymType.EOF){// 点之后还有内容
                errorHandle(50, token);
            }
        }
        else{// 缺少最后的点
            errorHandle(51, token);
        }
    }

    // <分程序> ::= [<常量说明部分>][<变量说明部分>][<过程说明部分>]<语句>
    private void block(Set<SymType> symTypes) {
        int symbolTableSize = symbolTable.getSymbolTable().size();
        int address_cp = address; // 保存之前的address
        //初始化本层的相关变量
        int start = symbolTable.getPtr(); // 本层变量声明的初始位置
        int pos = 0; //本层过程声明在符号表中的位置
        address = 3; //默认从3开始，前面存基地址、程序计数器等
        if(start > 0)
            pos = symbolTable.getInitPos(level);

        // 设置跳转指令，跳过声明部分，后面回填
        int tmpCodePtr = pcodeList.size();
        pcodeList.gen(Operator.JMP, 0, 0);

        while (true){
            if(token.getType() == SymType.CON){
                constantDeclare();
            }
            if(token.getType() == SymType.VAR){
                variableDeclare();
            }
            if(token.getType() == SymType.PROC){
                procedureDeclare(symTypes);
            }

            Set<SymType> tmp2 = new HashSet<>();
            tmp2.addAll(statementHead);
            isLegal(tmp2, symTypes, 7);
            if(!declarationHead.contains(token.getType()))
                break;
        }


        // 回填跳转地址
        pcodeList.getPcodes().get(tmpCodePtr).setA(pcodeList.size());
        pcodeList.gen(Operator.INT, 0, address); // 申请空间
        if(start != 0){
            // 如果不是主函数，则需要在符号表中的value填入该过程在Pcode代码中的起始位置
            symbolTable.getSymbolTable().get(pos).setVal(pcodeList.size() - 1);
        }

        Set<SymType> tmp = new HashSet<>();
        tmp.addAll(symTypes);
        tmp.add(SymType.SEMIC);
        tmp.add(SymType.END);
        statement(tmp);
        pcodeList.gen(Operator.OPR, 0, 0); // 过程结束
        address = address_cp;
        symbolTable.symbolTable = symbolTable.getSymbolTable().subList(0, symbolTableSize);
        isLegal(symTypes, new HashSet<SymType>(), 8);
    }

    // <常量说明部分> ::= const<常量定义>{,<常量定义>};
    private void constantDeclare(){
        if(token.getType() == SymType.CON){
            nextSym();
            constantDefine();
            while(token.getType() == SymType.COMMA || token.getType() == SymType.SYM){
                if(token.getType() == SymType.COMMA){
                    nextSym();
                }
                else{ // 缺少逗号
                    errorHandle(5, token);
                }
                constantDefine();
            }
            if(token.getType() == SymType.SEMIC)
                nextSym();
            else // 缺少 ;
                errorHandle(5, token);
        }
        else{ // 缺少 const
            errorHandle(52, token);
        }
    }

    // <常量定义> ::= <标识符>=<无符号整数>
    private void constantDefine(){
        String name; // 标识符的名字
        int value;   // 无符号整数的值
        if(token.getType() == SymType.SYM){// 标识符
            name = token.getToken();
            nextSym();
            if(token.getType() == SymType.EQU){// =
                nextSym();
                if(token.getType() == SymType.INT){// 无符号整数
                    value = Integer.parseInt(token.getToken());
                    if(symbolTable.inThisLevel(name, level)){
                        // 在当前层次已经定义过该标识符
                        errorHandle(53, token);
                    }
                    symbolTable.putConst(name, value, level, address);
                    nextSym();
                }
                else{// 赋值号后不是无符号整数
                    errorHandle(2, token);
                }
            }
            else{// 没有赋值号=
                errorHandle(3, token);
            }
        }
        else{// 标识符不合法
            errorHandle(54, token);
        }
    }

    // <变量说明部分>::= var<标识符>{,<标识符>};
    private void variableDeclare(){
        if(token.getType() ==  SymType.VAR){
            nextSym();
            if(token.getType() == SymType.SYM){
                String name = token.getToken();
                if(symbolTable.inThisLevel(name, level)){ //TODO 参数是address还是level？
                    // 在当前层次已经定义过该标识符
                    errorHandle(53, token);
                }
                symbolTable.putVar(name, level, address);
                address += addressIncrement;
                nextSym();
                while(token.getType() == SymType.COMMA || token.getType() == SymType.SYM){
                    if(token.getType() == SymType.COMMA){
                        nextSym();
                    }
                    else{ // 缺少逗号
                        errorHandle(5, token);
                    }
                    if(token.getType() == SymType.SYM){
                        name = token.getToken();
                        if(symbolTable.inThisLevel(name, level)){
                            // 在当前层次已经定义过该标识符
                            errorHandle(53, token);
                        }
                        symbolTable.putVar(name, level, address);
                        address += addressIncrement;
                        nextSym();
                    }
                    else{// 非法标识符
                        errorHandle(54, token);
                        return;
                    }
                }
                if(token.getType() != SymType.SEMIC){// 缺少 ;
                    errorHandle(5, token);
                }
                else{
                    nextSym();
                }
            }
            else{// 非法标识符
                errorHandle(54, token);
            }
        }
        else{// 缺少var
            errorHandle(52, token);
        }
    }

    // <过程说明部分> ::= <过程首部><分程序>;{<过程说明部分>}
    // <过程首部> ::= procedure<标识符>;
    private void procedureDeclare(Set<SymType> symTypes){
        if(token.getType() == SymType.PROC){
            nextSym();
            if(token.getType() == SymType.SYM){
                String name = token.getToken();
                if(symbolTable.inThisLevel(name, level)){
                    // 过程在同层中声明过
                    errorHandle(53, token);
                }
                symbolTable.putProc(name, level, address);
                level++;
                address += addressIncrement;
                nextSym();

                if(token.getType() == SymType.SEMIC){
                    nextSym();
                }
                else{ // 缺少 ;
                    errorHandle(5, token);
                }

                Set<SymType> tmp = new HashSet<>();
                tmp.addAll(symTypes);
                tmp.add(SymType.SEMIC);
                block(tmp);
                while (token.getType() == SymType.SEMIC || token.getType() == SymType.PROC){
                    if(token.getType() == SymType.SEMIC)
                        nextSym();
                    else // 缺少 ;
                        errorHandle(5, token);
                    level--;
                    Set<SymType> tmp2 = new HashSet<>();
                    tmp2.addAll(statementHead);
                    tmp2.add(SymType.SYM);
                    tmp2.add(SymType.PROC);
                    isLegal(tmp2, symTypes, 6);
                    procedureDeclare(symTypes);
                }
            }
            else{// 标识符不合法
                errorHandle(54, token);
            }
        }
    }

//    <语句> ::= <赋值语句>|<条件语句>|<当型循环语句>|<过程调用语句>|<读语句>|<写语句>|<复合语句>|<重复语句>|<空>
    private void statement(Set<SymType> symTypes){
        if(token.getType() == SymType.SYM){ // <赋值语句> ::= <标识符>:=<表达式>
            String name = token.getToken();
            nextSym();
            if(token.getType()==SymType.CEQU){ // :=
                nextSym();
                expression(symTypes);
                if(!symbolTable.isDefined(name, level)){ // 标识符未定义
                    errorHandle(11, token);
                }
                else{
                    Symbol symbol = symbolTable.getSymbol(name); // 在符号表中找到标识符
                    if(symbol.getKind() == SymbolKind.variable){ // 是变量
                        // 将栈顶内容存入变量
                        pcodeList.gen(Operator.STO, level - symbol.getLevel(), symbol.getAdr());
                    }
                    else{ // 向常量或过程赋值
                        errorHandle(12, token);
                    }
                }
            }
            else{
                errorHandle(13, token);
            }
        }
        else if(token.getType() == SymType.IF){ // <条件语句> ::= if<条件>then<语句>[else<语句>]
            /* [if]
               <condition>
               JPC addr1
               <statement>
               JMP addr2
            addr1:  [else]
               <statement>
            addr2:          */
            nextSym();
            Set<SymType> tmp = new HashSet<>();
            tmp.addAll(symTypes);
            tmp.add(SymType.THEN);
            condition(tmp);
            if(token.getType() == SymType.THEN){
                int pos1 = pcodeList.size(); // 指向JPC addr1
                pcodeList.gen(Operator.JPC, 0, 0);
                nextSym();
                Set<SymType> tmp2 = new HashSet<>();
                tmp2.addAll(symTypes);
                tmp2.add(SymType.ELSE);
                statement(tmp2);
                // 地址回填
                pcodeList.getPcodes().get(pos1).setA(pcodeList.size());
                if(token.getType() == SymType.ELSE){
                    int pos2 = pcodeList.size(); // 指向JMP addr2
                    pcodeList.gen(Operator.JMP, 0, 0);
                    nextSym();
                    statement(symTypes);
                    pcodeList.getPcodes().get(pos2).setA(pcodeList.size());
                }
            }
            else{ // if后没有then
                errorHandle(16, token);
            }
        }
        else if(token.getType() == SymType.WHILE){ // <当型循环语句> ::= while<条件>do<语句>
            /*  [while]
            addr2:  <condition>
                JPC addr3
                <statement>
                JMP addr2
            addr3:          */
            int pos1 = pcodeList.size(); // 指向 addr2
            nextSym();
            Set<SymType> tmp = new HashSet<>();
            tmp.addAll(symTypes);
            tmp.add(SymType.DO);
            condition(tmp);
            if(token.getType() == SymType.DO){
                int pos2 = pcodeList.size(); // 指向 JPC addr3
                pcodeList.gen(Operator.JPC, 0, 0);
                nextSym();
                statement(symTypes);
                pcodeList.gen(Operator.JMP, 0, pos1); // JMP addr2
                // 地址回填, JPC addr3
                pcodeList.getPcodes().get(pos2).setA(pcodeList.size());
            }
            else{ // while后没有do
                errorHandle(18, token);
            }
        }
        else if(token.getType() == SymType.CALL){ // <过程调用语句> ::= call<标识符>
            nextSym();
            if(token.getType() == SymType.SYM){
                String name = token.getToken(); //过程名
                if(symbolTable.isDefined(name, level)){
                    Symbol symbol = symbolTable.getSymbol(name);
                    if(symbol.getKind() == SymbolKind.procedure){
                        // 调用过程
                        pcodeList.gen(Operator.CAL, level - symbol.getLevel(), symbol.getVal());
                    }
                    else{ // 调用的标识符是常量或变量
                        errorHandle(15, token);
                    }
                }
                else{ // 过程未定义
                    errorHandle(11, token);
                }
                nextSym();
            }
            else{ // call后不是标识符
                errorHandle(14, token);
            }
        }
        else if(token.getType() == SymType.READ){ // <读语句> ::= read'('<标识符>{,<标识符>}')‘
            nextSym();
            if(token.getType() == SymType.LBR){ // (
                nextSym();
                if(token.getType() == SymType.SYM){
                    String name = token.getToken(); //标识符名
                    if(symbolTable.isDefined(name, level)){
                        Symbol symbol = symbolTable.getSymbol(name); //标识符
                        if(symbol.getKind() == SymbolKind.variable){ //是变量
                            // 从命令行读入一个输入存入变量
                            pcodeList.gen(Operator.RED, level-symbol.getLevel(), symbol.getAdr());
                        }
                        else{ //不是变量
                            errorHandle(55, token);
                            return;
                        }
                    }
                    else{ //标识符未定义
                        errorHandle(11, token);
                        return;
                    }
                }
                else{ // 参数应为标识符
                    errorHandle(54, token);
                    return;
                }

                nextSym();
                while(token.getType() == SymType.COMMA){
                    nextSym();
                    if(token.getType() == SymType.SYM) {
                        String name = token.getToken(); //标识符名
                        if(symbolTable.isDefined(name, level)){
                            Symbol symbol = symbolTable.getSymbol(name); //标识符
                            if(symbol.getKind() == SymbolKind.variable){ //是变量
                                // 从命令行读入一个输入置于栈顶
                                // 从命令行读入一个输入存入变量
                                pcodeList.gen(Operator.RED, level-symbol.getLevel(), symbol.getAdr());
                            }
                            else{ //不是变量
                                errorHandle(55, token);
                                return;
                            }
                        }
                        else{ //标识符未定义
                            errorHandle(11, token);
                            return;
                        }
                    }
                    else{ // 参数应为标识符
                        errorHandle(54, token);
                        return;
                    }
                    nextSym();
                }

                if(token.getType() == SymType.RBR){ // )
                    nextSym();
                }
                else{ // 缺少右括号
                    errorHandle(22, token);
                }
            }
            else{ // read后不是左括号
                errorHandle(40, token);
            }
        }
        else if(token.getType() == SymType.WRITE){ // <写语句> ::= write'('<标识符>{,<标识符>}')‘
            nextSym();
            if(token.getType() == SymType.LBR){ // (
                nextSym();
                Set<SymType> tmp = new HashSet<>();
                tmp.addAll(symTypes);
                tmp.add(SymType.RBR);
                tmp.add(SymType.COMMA);
                expression(tmp);
                pcodeList.gen(Operator.WRT, 0, 0);
                while(token.getType() == SymType.COMMA){
                    nextSym();
                    expression(tmp);
                    pcodeList.gen(Operator.WRT, 0, 0);
                }
                if(token.getType() == SymType.RBR){
                    nextSym();
                }
                else{ // 缺少右括号
                    errorHandle(22, token);
                }
            }
            else{ // write后不是左括号
                errorHandle(40, token);
            }
        }
        else if(token.getType() == SymType.BEGIN){ // <复合语句> ::= begin<语句>{;<语句>}end
            nextSym();
            Set<SymType> tmp = new HashSet<>();
            tmp.addAll(symTypes);
            tmp.add(SymType.SEMIC);
            tmp.add(SymType.END);
            statement(tmp);

            Set<SymType> tmp2 = new HashSet<>();
            tmp2.addAll(statementHead);
            tmp2.add(SymType.SEMIC);

            while(tmp2.contains(token.getType())){ // {;<语句>}
                if(token.getType() == SymType.SEMIC)
                    nextSym();
                else
                    errorHandle(10, token);
                statement(tmp);
            }


            if(token.getType() == SymType.END){
                nextSym();
            }
            else{ // 缺少end
                errorHandle(17, token);
            }

        }
        else if(token.getType() == SymType.REP){ // <重复语句> ::= repeat<语句>{;<语句>}until<条件>
            /*  [repeat]
            addr4:  <statement>
                [until]
                <condition>
                JPC addr4   */
            int pos = pcodeList.size(); // 指向addr4
            nextSym();
            Set<SymType> tmp = new HashSet<>();
            tmp.addAll(symTypes);
            tmp.add(SymType.SEMIC);
            tmp.add(SymType.UNT);
            statement(tmp);

            Set<SymType> tmp2 = new HashSet<>();
            tmp2.addAll(statementHead);
            tmp2.add(SymType.SEMIC);
            while(tmp2.contains(token.getType())){ // {;<语句>}
                if(token.getType() == SymType.SEMIC)
                    nextSym();
                else
                    errorHandle(10, token);
                statement(tmp);
            }
            if(token.getType() == SymType.UNT){
                nextSym();
                condition(symTypes);
                pcodeList.gen(Operator.JPC, 0, pos);
            }
            else{ // 缺少until
                errorHandle(56, token);
            }
        }
        isLegal(symTypes, new HashSet<SymType>(), 19);
    }

//    <条件> ::= <表达式><关系运算符><表达式>|odd<表达式>
//    <关系运算符> ::= =|<>|<|<=|>|>=
    private void condition(Set<SymType> symTypes){
        if(token.getType() == SymType.ODD){ // ODD<表达式>
            pcodeList.gen(Operator.OPR, 0, 6); // 判断奇偶
            nextSym();
            expression(symTypes);
        }
        else{ // <表达式><关系运算符><表达式>
            Set<SymType> tmp2 = new HashSet<>();
            tmp2.addAll(symTypes);
            tmp2.add(SymType.EQU);
            tmp2.add(SymType.NEQE);
            tmp2.add(SymType.LES);
            tmp2.add(SymType.LESE);
            tmp2.add(SymType.LAR);
            tmp2.add(SymType.LARE);
            expression(tmp2);
            SymType tmp = token.getType();
            nextSym();
            expression(symTypes);
            switch (tmp){
                case EQU:   // =
                    pcodeList.gen(Operator.OPR, 0, 8); // 判断是否相等
                    break;
                case NEQE:  // <>
                    pcodeList.gen(Operator.OPR, 0, 9); // 判断是否不等
                    break;
                case LES:   // <
                    pcodeList.gen(Operator.OPR, 0, 10); // 判断是否小于
                    break;
                case LESE:  // <=
                    pcodeList.gen(Operator.OPR, 0, 13); // 判断是否小于等于
                    break;
                case LAR:   // >
                    pcodeList.gen(Operator.OPR, 0, 12); // 判断是否大于
                    break;
                case LARE:  // >=
                    pcodeList.gen(Operator.OPR, 0, 11); // 判断是否大于等于
                    break;
                default:    // 不合法的关系运算符
                    errorHandle(20, token);
            }
        }
    }

//    <表达式> ::= [+|-]<项>{<加法运算符><项>}
//    <加法运算符> ::= +|-
    private void expression(Set<SymType> symTypes){
        Set<SymType> tmp = new HashSet<>();
        tmp.addAll(symTypes);
        tmp.add(SymType.ADD);
        tmp.add(SymType.SUB);
        SymType type = token.getType();
        if(type == SymType.ADD || type == SymType.SUB)
            nextSym();
        term(tmp);
        if(type == SymType.SUB) // 负数
            pcodeList.gen(Operator.OPR, 0, 1);
        while(token.getType() == SymType.ADD || token.getType() == SymType.SUB){
            type = token.getType(); // positive or negative
            nextSym();
            term(tmp);
            if(type == SymType.ADD) // 加
                pcodeList.gen(Operator.OPR, 0, 2);
            else // 减
                pcodeList.gen(Operator.OPR, 0, 3);
        }
    }

//    <项> ::= <因子>{<乘法运算符><因子>}
//    <乘法运算符> ::= *|/
    private void term(Set<SymType> symTypes){
        Set<SymType> tmp2 = new HashSet<>();
        tmp2.addAll(symTypes);
        tmp2.add(SymType.MUL);
        tmp2.add(SymType.DIV);
        factor(tmp2);
        while(token.getType() == SymType.MUL || token.getType() == SymType.DIV){
            SymType tmp = token.getType(); // * or /
            nextSym();
            factor(tmp2);
            if(tmp == SymType.MUL) // 乘
                pcodeList.gen(Operator.OPR, 0, 4);
            else // 除
                pcodeList.gen(Operator.OPR, 0, 5);

        }
    }

//    <因子> ::= <标识符>|<无符号整数>|'('<表达式>')‘
    private void factor(Set<SymType> symTypes){
        isLegal(factorHead, symTypes, 24);
        if(token.getType() == SymType.INT){ // 无符号整数
            // 取常量放到数据栈栈顶
            pcodeList.gen(Operator.LIT, 0, Integer.parseInt(token.getToken()));
            nextSym();
        }
        else if(token.getType() == SymType.LBR){ // (表达式)
            nextSym();
            Set<SymType> tmp = new HashSet<>();
            tmp.addAll(symTypes);
            tmp.add(SymType.RBR);
            expression(tmp);
            if(token.getType() == SymType.RBR){
                nextSym();
            }
            else{ // 缺少右括号
                errorHandle(22, token);
            }
        }
        else if(token.getType() == SymType.SYM){ // 标识符
            String name = token.getToken();
            if(!symbolTable.isDefined(name, level)){ // 标识符未定义
                errorHandle(11, token);
                while(token.getType() != SymType.SEMIC)
                    nextSym();
                return;
            }
            else{
                Symbol tmp = symbolTable.getSymbol(name); // 找到该标识符
                if(tmp.getKind() == SymbolKind.variable){ // 是变量
                    // 取变量放到数据栈栈顶
                    pcodeList.gen(Operator.LOD, level - tmp.getLevel(), tmp.getAdr());
                }
                else if(tmp.getKind() == SymbolKind.constant){ // 是常量
                    // 取常量放到数据栈栈顶
                    pcodeList.gen(Operator.LIT, 0, tmp.getVal());
                }
                else{ // 是过程
                    errorHandle(21, token);
                    return;
                }
            }
            nextSym();
        }
        isLegal(symTypes, new HashSet<SymType>(), 23);
    }

    // 读进一个token, 若是非法符号则跳过
    private void nextSym(){
        token = tokens.get(ptr++);
        while(token.getType() == SymType.ERROR){
            errorHandle(57, token);
            token = tokens.get(ptr++);
        }
    }

    // 判断当前token是否为语句的头部
    private boolean isHeadOfStatement() {
        return  token.getType() == SymType.IF   ||
                token.getType() == SymType.WHILE||
                token.getType() == SymType.CALL ||
                token.getType() == SymType.READ ||
                token.getType() == SymType.WRITE||
                token.getType() == SymType.BEGIN||
                token.getType() == SymType.SYM  ||
                token.getType() == SymType.REP;
    }

    /**
     * 错误处理
     * @param errorNo  错误编号
     * @param token 标识符
     */
    private void errorHandle(int errorNo, Token token){
        switch (errorNo){
            case 1:
                errors.add(new Error(errorNo, "应是 = 而不是 :=", token));
                break;
            case 2:
                errors.add(new Error(errorNo, "= 后应为数", token));
                break;
            case 3:
                errors.add(new Error(errorNo, "标识符后应为 =", token));
                break;
            case 4:
                errors.add(new Error(errorNo, "const, var, procedure 后应为标识符", token));
                break;
            case 5:
                errors.add(new Error(errorNo, "漏掉逗号或分号", token));
                break;
            case 6:
                errors.add(new Error(errorNo, "过程说明后的符号不正确", token));
                break;
            case 7:
                errors.add(new Error(errorNo, "应为语句", token));
                break;
            case 8:
                errors.add(new Error(errorNo, "程序体内语句部分后的符号不正确", token));
                break;
            case 9:
                errors.add(new Error(errorNo, "应为句号", token));
                break;
            case 10:
                errors.add(new Error(errorNo, "语句之间漏分号", token));
                break;
            case 11:
                errors.add(new Error(errorNo, "标识符未说明", token));
                break;
            case 12:
                errors.add(new Error(errorNo, "不可向常量或过程赋值", token));
                break;
            case 13:
                errors.add(new Error(errorNo, "应为赋值运算符:=", token));
                break;
            case 14:
                errors.add(new Error(errorNo, "call后应为标识符", token));
                break;
            case 15:
                errors.add(new Error(errorNo, "不可调用常量或变量", token));
                break;
            case 16:
                errors.add(new Error(errorNo, "应为then", token));
                break;
            case 17:
                errors.add(new Error(errorNo, "应为分号或end", token));
                break;
            case 18:
                errors.add(new Error(errorNo, "应为do", token));
                break;
            case 19:
                errors.add(new Error(errorNo, "语句后的符号不正确", token));
                break;
            case 20:
                errors.add(new Error(errorNo, "应为关系运算符", token));
                break;
            case 21:
                errors.add(new Error(errorNo, "表达室内不可有过程标识符", token));
                break;
            case 22:
                errors.add(new Error(errorNo, "漏右括号", token));
                break;
            case 23:
                errors.add(new Error(errorNo, "因子后不能以此符号开始", token));
                break;
            case 24:
                errors.add(new Error(errorNo, "表达式不能以此符号开始", token));
                break;
            case 30:
                errors.add(new Error(errorNo, "这个数太大", token));
                break;
            case 40:
                errors.add(new Error(errorNo, "应为左括号", token));
                break;
            case 50:  // 应该用.作为结束，但是后面还有内容
                errors.add(new Error(errorNo, "在.之后还有多余内容", token));
                break;
            case 51:  // 结尾没有.
                errors.add(new Error(errorNo, "代码没有以.结尾", token));
                break;
            case 52:  // 常量定义不是const开头，变量定义不是var开头
                errors.add(new Error(errorNo, "应为const或var", token));
                break;
            case 53:  // 在当前层次已经定义过该标识符
                errors.add(new Error(errorNo, "重复定义", token));
                break;
            case 54:  // 标识符不合法
                errors.add(new Error(errorNo, "标识符不合法", token));
                break;
            case 55:  // read或write中的标识符不为var
                errors.add(new Error(errorNo, "read或write中的标识符应为变量", token));
                break;
            case 56:  // 应为until
                errors.add(new Error(errorNo, "应为until", token));
                break;
            case 57:
                errors.add(new Error(errorNo, "非法符号", token));
            case 58:
                errors.add(new Error(errorNo, "write中的标识符应为常量或变量", token));
        }
    }

    public List<Error> getErrors() {
        return errors;
    }
    public List<Pcode> getPcodes() {
        return pcodeList.getPcodes();
    }

    private void isLegal(Set<SymType> s1, Set<SymType> s2, int n){
        if(!s1.contains(token.getType())){
            errorHandle(n, token);
            while(!s1.contains(token.getType()) && !s2.contains(token.getType())){
                if(ptr == tokens.size() - 1)
                    break;
                nextSym();
            }

        }
    }
}
