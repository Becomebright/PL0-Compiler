package bright.OPG;

import javafx.util.Pair;

import java.util.*;

/* -1代表<，0代表=，1代表>，2代表不允许 */
class OPG_Grammar {  //TODO #的处理, VnMap Key相同处理
    private int [][]matrix;
    private int vtNum;
    private int vnNum;
    private char []Vn;
    private Map<Character, Integer> Vt = new HashMap<>();
    private Map<String, Character> grammar = new HashMap<>();   //<规则右部, 规则左部>
    private Map<String, Character> fGrammar = new HashMap<>();  //规则右部的Vn被替换成$
    private Map<Character, Set<Character>> first = new HashMap<>();
    private Map<Character, Set<Character>> last = new HashMap<>();

    OPG_Grammar(List<String> grammars) {
        grammars.add("@->#" + grammars.get(0).charAt(0) + "#");
        int res = init(grammars);
        if(res == 0){
            System.out.println("init时出现错误");
        }
    }

    private int init(List<String> grammars) {  // 生成优先关系矩阵等数据结构
        vnNum = grammars.size();
        Vn = new char[vnNum];

        // 求Vn符号集
        int i=0;
        for(String g : grammars){
            char U = g.charAt(0);
            first.put(U, new HashSet<>());
            last.put(U, new HashSet<>());
            Vn[i++] = U;
        }

        // 求Vn规则集
        i = 0;
        for(String g : grammars){
            int start = g.indexOf("->") + 2;
            int end = start + 1;
            while(end < g.length()){
                if(g.charAt(end) == '|'){
                    grammar.put(g.substring(start, end), Vn[i]);
                    fGrammar.put(formVnRight(g.substring(start, end)), Vn[i]);
                    start = end + 1;
                    end += 2;
                }
                else end++;
            }
            grammar.put(g.substring(start, end), Vn[i]);
            fGrammar.put(formVnRight(g.substring(start, end)), Vn[i]);
            i++;
        }

        // 求Vt集
        int cnt = 0;  // 给每个Vt一个编号，从0开始
        for(String s : grammar.keySet()){  // 遍历Vn Map的Key(String类型)，其中不是Vn的字符即为Vt
            for(char c : s.toCharArray()){
                if(!isVn(c) && !isVt(c))  // 既不属于Vn，且此时还未插入Vt
                    Vt.put(c, cnt++);
            }
        }
        vtNum = Vt.size();

        // 求优先关系矩阵
        makeFirst();
        makeLast();
        matrix = new int[vtNum][vtNum];
        makeMatrix();

        //printResult(); // 输出Vt和优先关系矩阵

        return 1;
    }

    // 输出Vt和优先关系矩阵
    private void printResult() {
        for(char c : Vt.keySet())
            System.out.print(String.valueOf(c) + Vt.get(c) + " ");
        System.out.println("");
        for(int k=0; k<vtNum; k++) {
            for (int j = 0; j < vtNum; j++)
                System.out.print(matrix[k][j] + " ");
            System.out.println("");
        }
    }

    private void insert(Map<Pair<Character, Character>, Boolean> F,
                        Stack<Pair<Character, Character>> stack, char U, char b){
        if(!F.get(new Pair<>(U, b))){
            F.put(new Pair<>(U, b), true);
            stack.push(new Pair<>(U, b));
        }
    }

    private void makeFirst() {
        Stack<Pair<Character, Character>> stack = new Stack<>();
        Map<Pair<Character, Character>, Boolean> F = new HashMap<>();
        for(char U : grammar.values())
            for(char b : Vt.keySet())
                F.put(new Pair<>(U, b), false);
        for(Map.Entry<String, Character> g : grammar.entrySet()){  // U->s(b.../Vb...)
            char U = g.getValue();
            String s = g.getKey();
            if(isVt(s.charAt(0))){
                char b = s.charAt(0);
                 insert(F, stack, U, b);
            }
            else if(s.length()>1 && isVn(s.charAt(0)) && isVt((s.charAt(1)))){
                char b = s.charAt(1);
                insert(F, stack, U, b);
            }
        }
        while(!stack.empty()){
            Pair<Character, Character> p = stack.pop();
            char V = p.getKey();
            char b = p.getValue();
            for(Map.Entry<String, Character> g : grammar.entrySet()){  // U->s(V...)
                char U = g.getValue();
                String s = g.getKey();
                if(V == s.charAt(0))
                    insert(F, stack, U, b);
            }
        }
        // F[U, b]=true则b加入first(U)
        for(Map.Entry<Pair<Character, Character>, Boolean> f : F.entrySet()){
            if(f.getValue()){
                char U = f.getKey().getKey();
                char b = f.getKey().getValue();
                first.get(U).add(b);
            }
        }
    }

    private void makeLast() {
        Stack<Pair<Character, Character>> stack = new Stack<>();
        Map<Pair<Character, Character>, Boolean> F = new HashMap<>();
        for(char U : grammar.values())
            for(char b : Vt.keySet())
                F.put(new Pair<>(U, b), false);
        for(Map.Entry<String, Character> g : grammar.entrySet()){  // U->s(...b/...bV)
            char U = g.getValue();
            String s = g.getKey();
            if(isVt(s.charAt(s.length()-1))){
                char b = s.charAt(s.length()-1);
                insert(F, stack, U, b);
            }
            else if(s.length()>1 && isVn(s.charAt(s.length()-1)) && isVt((s.charAt(s.length()-2)))){
                char b = s.charAt(s.length()-2);
                insert(F, stack, U, b);
            }
        }
        while(!stack.empty()){
            Pair<Character, Character> p = stack.pop();
            char V = p.getKey();
            char b = p.getValue();
            for(Map.Entry<String, Character> g : grammar.entrySet()){  // U->s(...V)
                char U = g.getValue();
                String s = g.getKey();
                if(V == s.charAt(s.length()-1))
                    insert(F, stack, U, b);
            }
        }
        // F[U, b]=true则b加入last(U)
        for(Map.Entry<Pair<Character, Character>, Boolean> f : F.entrySet()){
            if(f.getValue()){
                char U = f.getKey().getKey();
                char b = f.getKey().getValue();
                last.get(U).add(b);
            }
        }
    }

    private void makeMatrix(){
        for(int i=0; i<vtNum; i++)
            for(int j=0; j<vtNum; j++)
                matrix[i][j] = 2;  // 初始化为2
        for(String s : grammar.keySet()){  // 对于每条规则
            char []x = s.toCharArray();
            int n = s.length();
            for(int i=0; i<n-1; i++){
                if(isVt(x[i]) && isVt(x[i+1]))
                    matrix[Vt.get(x[i])][Vt.get(x[i+1])] = 0; // x[i] = x[i+1]
                if(i<n-2 && isVt(x[i]) && isVt(x[i+2]) && isVn(x[i+1]))
                    matrix[Vt.get(x[i])][Vt.get(x[i+2])] = 0; // x[i] = x[i+2]
                if(isVt(x[i]) && isVn(x[i+1]))
                    for(char b : first.get(x[i+1]))
                        matrix[Vt.get(x[i])][Vt.get(b)] = -1; // x[i] < b
                if(isVn(x[i]) && isVt(x[i+1]))
                    for(char a : last.get(x[i]))
                        matrix[Vt.get(a)][Vt.get(x[i+1])] = 1; // a > x[i+1]
            }
        }
        matrix[Vt.get('#')][Vt.get('#')] = 2; // #与#之间为2
    }

    /**
     * @param s 输入规则右部
     * @return 把规则右部的Vn替换成$
     */
    private String formVnRight(String s) {
        StringBuilder tmp = new StringBuilder(s);
        for(int i=0; i<s.length(); i++){
            if(isVn(s.charAt(i)))
                tmp.replace(i,i+1,"$");
        }
        return tmp.toString();
    }

    int compare(char a, char b){
        if(a == '#'){
            if(b == '#') return 0;
            else  return -1;
        }
        else if(b == '#') return 1;
        else return matrix[Vt.get(a)][Vt.get(b)];
    }

    boolean isVt(char c) {
        return Vt.containsKey(c);
    }

    boolean isVn(char c){
        for(int i=0; i<vnNum; i++)
            if(Vn[i] == c) return true;
        return false;
    }

    int[][] getMatrix() {
        return matrix;
    }

    int getVtNum() {
        return vtNum;
    }

    Map<Character, Integer> getVt() {
        return Vt;
    }

    public Map<Character, Set<Character>> getFirst() {
        return first;
    }

    public Map<Character, Set<Character>> getLast() {
        return last;
    }

    /**
     * @param s 输入要规约的最左素短语
     * @return 规约成功则返回规约到的Vn；规约失败返回0
     */
    char reduce(String s){
        String tmp = formVnRight(s);  // 将最左素短语中的Vn替换成$
        return fGrammar.get(tmp);
    }

    char [][]getCMatrix(){
        char [][]m = new char[vtNum][vtNum];
        for(int i=0; i<vtNum; i++){
            for(int j=0; j<vtNum; j++){
                switch (matrix[i][j]){
                    case -1:
                        m[i][j] = '<';
                        break;
                    case 0:
                        m[i][j] = '=';
                        break;
                    case 1:
                        m[i][j] = '>';
                        break;
                    case 2:
                        m[i][j] = ' ';
                        break;
                }
            }
        }
        return m;
    }
}
