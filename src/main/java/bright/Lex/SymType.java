package bright.Lex;

public enum SymType {
    CON,  VAR,   PROC,     ODD, IF, THEN, ELSE, WHILE, CALL, BEGIN, END,  REP,   UNT,   READ, WRITE, DO,
/*  const var   procedure  odd  if  then  else  while  call  begin  end  repeat  until  read  write  do */
    EQU, LES, LESE, LARE, LAR, NEQE, ADD, SUB, MUL, DIV, CEQU,
/*   =    <    <=    >=     >   <>    +    -    *    /	  :=    */
    SYM, INT, REAL,
/* 标识符，常量整数   常量浮点数*/
    COMMA, SEMIC, POI, LBR, RBR, COL,
/*   ,      ;      .    (    )    :	 */
    EOF,
/*  end of file	 */
    ERROR;

    public String getTypeName(){
        if(this.compareTo(SymType.CON)>=0 && this.compareTo(SymType.WRITE)<=0)
            return "关键字";
        else if(this.compareTo(SymType.EQU)>=0 && this.compareTo(SymType.CEQU)<=0)
            return "运算符";
        else if(this.compareTo(SymType.SYM)==0)
            return "标识符";
        else if(this.compareTo(SymType.INT)==0)
            return "常整数";
        else if(this.compareTo(SymType.REAL)==0)
            return "常小数";
        else if(this.compareTo(SymType.COMMA)>=0 && this.compareTo(SymType.COL)<=0)
            return "分界符";
        else
            return "其它";
    }
}
