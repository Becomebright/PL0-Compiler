package bright.Compiler;

import java.util.ArrayList;
import java.util.List;

public class Interpreter {
    private final int STACK_SIZE = 1000;
    private int[] dataStack = new int[STACK_SIZE];
    private List<Pcode> pcodeList;
    private List<Integer> input;
    private int inputPtr = 0;
    //private int inputPtr;
    private List<String> output;

    public Interpreter() {
    }
    public Interpreter(List<Pcode> pcodeList, List<Integer> input) {
        this.pcodeList = pcodeList;
        this.input = input;
        output = new ArrayList<>();
    }

    public void interpret(){
        int pc = 0; //程序计数器
        int base = 0; // 当前基地址
        int top = 0; // 运行栈栈顶

        do{
            Pcode currentPcode = pcodeList.get(pc);
            pc++;
            if(currentPcode.getF() == Operator.LIT){ //LIT 0, a	取常量a放到数据栈栈顶
                dataStack[top++] = currentPcode.getA();
            }
            else if(currentPcode.getF() == Operator.OPR){
                switch (currentPcode.getA()){
                    case 0: // OPR 0 0	过程调用结束后,返回调用点并退栈
                        top = base;
                        pc = dataStack[base + 2];
                        base = dataStack[base];
                        break;
                    case 1: // OPR 0 1	栈顶元素取反
                        dataStack[top - 1] = -dataStack[top - 1];
                        break;
                    case  2: // OPR 0 2	次栈顶与栈顶相加，退两个栈元素，结果值进栈
                        dataStack[top - 2] = dataStack[top - 1] + dataStack[top - 2];
                        top--;
                        break;
                    case 3: // OPR 0 3	次栈顶减去栈顶，退两个栈元素，结果值进栈
                        dataStack[top - 2] = dataStack[top - 2] - dataStack[top - 1];
                        top--;
                        break;
                    case 4: //OPR 0 4   次栈顶乘以栈顶，退两个栈元素，结果值进栈
                        dataStack[top - 2] = dataStack[top - 1] * dataStack[top - 2];
                        top--;
                        break;
                    case 5: //OPR 0 5   次栈顶除以栈顶，退两个栈元素，结果值进栈
                        dataStack[top - 2] = dataStack[top - 2] / dataStack[top - 1];
                        top--;
                        break;
                    case 6: //OPR 0 6   栈顶元素的奇偶判断，结果值在栈顶
                        dataStack[top - 1] = dataStack[top - 1] % 2;
                        break;
                    case 7:
                        break;
                    case 8: //OPR 0 8   次栈顶与栈顶是否相等，退两个栈元素，结果值进栈
                        if (dataStack[top - 2] == dataStack[top - 1]) {
                            dataStack[top - 2] = 1;
                        } else {
                            dataStack[top - 2] = 0;
                        }
                        top--;
                        break;
                    case 9: //OPR 0 9   次栈顶与栈顶是否不等，退两个栈元素，结果值进栈
                        if (dataStack[top - 2] != dataStack[top - 1]) {
                            dataStack[top - 2] = 1;
                        } else {
                            dataStack[top - 2] = 0;
                        }
                        top--;
                        break;
                    case 10: //OPR 0 10  次栈顶是否小于栈顶，退两个栈元素，结果值进栈
                        if (dataStack[top - 2] < dataStack[top - 1]) {
                            dataStack[top - 2] = 1;
                        } else {
                            dataStack[top - 2] = 0;
                        }
                        top--;
                        break;
                    case 11: //OPR 0 11    次栈顶是否大于等于栈顶，退两个栈元素，结果值进栈
                        if (dataStack[top - 2] >= dataStack[top - 1]) {
                            dataStack[top - 2] = 1;
                        } else {
                            dataStack[top - 2] = 0;
                        }
                        top--;
                        break;
                    case 12: //OPR 0 12  次栈顶是否大于栈顶，退两个栈元素，结果值进栈
                        if (dataStack[top - 2] > dataStack[top - 1]) {
                            dataStack[top - 2] = 1;
                        } else {
                            dataStack[top - 2] = 0;
                        }
                        top--;
                        break;
                    case 13: //OPR 0 13  次栈顶是否小于等于栈顶，退两个栈元素，结果值进栈
                        if (dataStack[top - 2] <= dataStack[top - 1]) {
                            dataStack[top - 2] = 1;
                        } else {
                            dataStack[top - 2] = 0;
                        }
                        top--;
                        break;
                }
            }
            else if(currentPcode.getF() == Operator.RED){
                // RED l, a
                if(inputPtr == input.size()){ //已经没有输入了
                    output.add("输入不合法!");
                    return;
                }
                dataStack[currentPcode.getA() + getBase(base, currentPcode.getL())] = input.get(inputPtr++);
            }
            else if(currentPcode.getF() == Operator.WRT){
                // WRT 0, 0
                System.out.println(dataStack[top - 1]);
                output.add(String.valueOf(dataStack[top - 1]));
            }
            else if(currentPcode.getF() == Operator.LOD){
                // LOD l, a	取变量放到数据栈栈顶(相对地址为a,层次差为l)
                dataStack[top++] = dataStack[currentPcode.getA() + getBase(base, currentPcode.getL())];
            }
            else if (currentPcode.getF() == Operator.STO) {
                //STO：将运行栈S的栈顶内容送入某个变量单元中，A段为变量所在说明层中的相对位置。
                dataStack[currentPcode.getA() + getBase(base, currentPcode.getL())] = dataStack[top - 1];
                top--;
            }
            else if (currentPcode.getF() == Operator.CAL) {
                //CAL：调用过程，这时A段为被调用过程的过程体（过程体之前一条指令）在目标程序区的入口地址。
                //跳转时，将该层基地址，跳转层基地址，pc指针保存在栈中
                //基地址base变为此时栈顶top，pc指向要跳转的地方
                //不修改top，因为前面代码已经将address+3，生成Pcode后会产生INT语句，修改top值
                dataStack[top] = base;
                dataStack[top + 1] = getBase(base, currentPcode.getL());
                dataStack[top + 2] = pc;
                base = top;
                pc = currentPcode.getA();
            }
            else if (currentPcode.getF() == Operator.INT) {
                //INT：为被调用的过程（包括主过程）在运行栈S中开辟数据区，这时A段为所需数据单元个数（包括三个连接数据）；L段恒为0。
                top = top + currentPcode.getA();
            }
            else if (currentPcode.getF() == Operator.JMP) {
                //JMP：无条件转移，这时A段为转向地址（目标程序）。
                pc = currentPcode.getA();
            }
            else if (currentPcode.getF() == Operator.JPC) {
                //JPC：条件转移，当运行栈S的栈顶的布尔值为假（0）时，则转向A段所指目标程序地址；否则顺序执行。
                if (dataStack[top - 1] == 0) {
                    pc = currentPcode.getA();
                }
            }
        }while (pc != 0);
    }

    /**
     * @param base  该层的基地址
     * @param level 层差为level
     * @return      层差为level层的基地址
     */
    private int getBase(int base, int level) {
        int oldBase = base;
        while(level > 0){
            oldBase = dataStack[oldBase + 1];
            level--;
        }
        return oldBase;
    }

    public List<String> getOutput() {
        return output;
    }
}