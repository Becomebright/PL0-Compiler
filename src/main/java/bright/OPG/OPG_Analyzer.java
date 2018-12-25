package bright.OPG;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OPG_Analyzer {
    public class AnalyzeResult{
        private int vtNum;
        private List<Character> vts;
        private Map<Character, Set<Character>> firstVt;
        private Map<Character, Set<Character>> lastVt;
        private char[][] matrix;
        private List<StepResult> stepResults = new ArrayList<>();
        private boolean error = false;

        AnalyzeResult(int vtNum, Map<Character, Set<Character>> firstVt, Map<Character, Integer> vt,
                      Map<Character, Set<Character>> lastVt, int[][] iMatrix) {
            this.vtNum = vtNum;
            firstVt.remove('@');
            lastVt.remove('@');
            this.firstVt = firstVt;
            this.lastVt = lastVt;
            this.vts = new ArrayList<>(vt.keySet());
            this.matrix = getCMatrix(iMatrix);
        }

        void setError(){
            this.error = true;
        }
        public int getVtNum() {
            return vtNum;
        }
        public List<Character> getVts() {
            return vts;
        }
        public Map<Character, Set<Character>> getFirstVt() {
            return firstVt;
        }
        public Map<Character, Set<Character>> getLastVt() {
            return lastVt;
        }
        public char[][] getMatrix() {
            return matrix;
        }
        public List<StepResult> getStepResults() {
            return stepResults;
        }
        public boolean isError() {
            return error;
        }
        private char [][]getCMatrix(int [][]matrix){
            char [][]m = new char[vtNum+1][vtNum+1];
            // 记录vts中第i出现的vt的编号，这么做是因为matrix是按vt的编号排的，但vts中vt的顺序却不按编号，这样输出时会错乱。
            int []vtNo = new int[vtNum];
            for(int i=0; i<vtNum; i++)
                vtNo[i] = grammar.getVt().get(vts.get(i));
            for(int row=0; row<vtNum; row++) {
                m[0][row+1] = m[row+1][0] = vts.get(row);
                for(int col=0; col<vtNum; col++){
                    switch (matrix[vtNo[row]][vtNo[col]]){
                        case -1:
                            m[row+1][col+1] = '<';
                            break;
                        case 0:
                            m[row+1][col+1] = '=';
                            break;
                        case 1:
                            m[row+1][col+1] = '>';
                            break;
                        case 2:
                            m[row+1][col+1] = ' ';
                            break;
                    }
                }
            }
            return m;
        }
    }


    public class StepResult {
        private int step = 0;            // 当前步数     2
        private String stack = "";       // 栈内情况     "#i+"
        private String relation = "";    // 符号关系     "# < i"
        private String action = "";      // 当前操作     "移进 i"
        private String input = "";       // 剩余输入串   "i+i#"
        private String error = "";       // 错误信息

        public int getStep() {
            return step;
        }
        void setStep(int step) {
            this.step = step;
        }
        public String getStack() {
            return stack;
        }
        void setStack(String stack) {
            this.stack = stack;
        }
        public String getRelation() {
            return relation;
        }
        void setRelation(char a, int relation, char b) {
            char r;
            if(relation == -1)
                r = '<';
            else if(relation == 0)
                r = '=';
            else if(relation == 1)
                r = '>';
            else r = ' ';
            this.relation = a + " " + r + " " + b;
        }
        public String getAction() {
            return action;
        }
        void setAction(String action) {
            this.action = action;
        }
        public String getInput() {
            return input;
        }
        void setInput(String input) {
            this.input = input;
        }
        public String getError() {
            return error;
        }
        void setError(String error) {
            this.error = error;
        }
    }


    private List<Character> stack = new ArrayList<>();
    private OPG_Grammar grammar;
    private int ptr = 0;
    public AnalyzeResult result;

    public OPG_Analyzer(List<String> grammars) {
        stack.add('#');
        grammar = new OPG_Grammar(grammars);
        result = new AnalyzeResult(grammar.getVtNum(), grammar.getFirst(),
               grammar.getVt() , grammar.getLast(), grammar.getMatrix());
    }

    /**
     * @param sentence 输入要分析的句子
     * @return 返回AnalyzeResult的List，若分析成功则最后一个元素的action被设置，否则error被设置
     */
    public AnalyzeResult analyze(String sentence) {  //TODO 规约有bug，如何直接规约素短语？ 按照文法规约是不够的，要有语法树
        List<StepResult> stepResults = result.stepResults;
        for (int ptr = 0, step = 1; ptr < sentence.length(); ++step) {
            StepResult stepResult = new StepResult();
            char ch = sentence.charAt(ptr);  // 当前读入的字符
            char topVt = getStackTopVt();  //栈顶Vt
            int relation = grammar.compare(topVt, ch);

            stepResult.setStep(step);
            stepResult.setStack(stack.toString());
            stepResult.setInput(sentence.substring(ptr));
            stepResult.setRelation(topVt, relation, ch);

            switch (relation) {
                case -1: // <
                    stack.add(ch);
                    stepResult.setAction("移进 " + ch);
                    //System.out.println("移进" + ch);
                    ++ptr;
                    break;
                case 0:  // =
                    if (ch == '#') {
                        if (topVt == '#') {
                            stepResult.setAction("分析成功！");
                            //System.out.println("规约成功0");
                            stepResults.add(stepResult);
                            return result;  // 规约成功
                        } else {
                            //TODO 错误处理，规约失败？
                            stepResult.setError("失败: 读到#时栈未空");
//                            System.out.println("失败: 读到#时栈未空");
//                            System.out.print("栈内: ");
//                            for (Character aVtStack : stack)
//                                System.out.print(aVtStack);
                            stepResults.add(stepResult);
                            result.setError();
                            return result;
                        }
                    }
                    stack.add(ch);
                    stepResult.setAction("移进" + ch);
                    //System.out.println("移进" + ch);
                    ++ptr;
                    break;
                case 1:  // >
                    String lpp = getLPP();
                    char vn = grammar.reduce(lpp);
                    if (vn == 0) {
                        //TODO 错误处理，规约失败？
                        stepResult.setError("失败: 规约不了 " + lpp);
                        //System.out.println("失败: 规约不了 " + lpp);
                        stepResults.add(stepResult);
                        result.setError();
                        return result;
                    }
                    stack.add(vn);
                    stepResult.setAction("规约: " + lpp + " ==> " + vn);
                    //System.out.println("规约: " + lpp + " ==> " + vn);
                    break;
            }
            stepResults.add(stepResult);
        }
        if (getStackTopVt() == '#') {
            return result;
        }
        //TODO 错误处理，规约失败？
        result.setError();
        //System.out.println("失败");
        return result;
    }

    /**
     * @return 返回栈顶的vt
     */
    private char getStackTopVt(){
        for(int i = stack.size()-1; i>=0; i--){  // 找到栈顶vt
            if(grammar.isVt(stack.get(i))){
                return stack.get(i);
            }
        }
        return ' ';
    }

    /**
     * @return 返回栈顶vt的index
     */
    private int getStackTopVtIndex(){
        for(int i = stack.size()-1; i>=0; i--){  // 找到栈顶vt
            if(grammar.isVt(stack.get(i))){
                return i;
            }
        }
        return -1;
    }

    /**
     * @return 弹出并返回栈顶元素
     */
    private char pop(){
        char res = stack.get(stack.size() - 1);
        stack.remove(stack.size() - 1);
        return res;
    }

    /**
     * @return 返回栈顶元素
     */
    private char top(){
        return stack.get(stack.size()-1);
    }

    /**
     * @return 返回最左素短语
     */
    private String getLPP() {
        StringBuilder res = new StringBuilder();
        int topVtIndex = getStackTopVtIndex();
        char topVt = stack.get(topVtIndex);
        for(int i=stack.size()-1; i>=topVtIndex; i--)
            res.append(pop());
        for(char top = top(); stack.size() > 0; top = top()){
            if(grammar.isVn(top))  // 是Vn
                res.append(pop());
            else if(grammar.compare(top, topVt) == 0){  // 是Vt且=
                res.append(pop());
                topVt = top;
            }
            else
                break;
        }
        res.reverse();
        return res.toString();
    }
}
