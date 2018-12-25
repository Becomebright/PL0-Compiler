package bright.Compiler;

public class Pcode{
    // f l, a
    private Operator f;
    private int l;
    private int a;

    Pcode(Operator f, int l, int a) {
        this.f = f;
        this.l = l;
        this.a = a;
    }
    public Operator getF() {
        return f;
    }
    public void setF(Operator f) {
        this.f = f;
    }
    public int getL() {
        return l;
    }
    public void setL(int l) {
        this.l = l;
    }
    public int getA() {
        return a;
    }
    void setA(int a) {
        this.a = a;
    }

    @Override
    public String toString() {
        return f + " " + l + ", " + a;
    }
}
