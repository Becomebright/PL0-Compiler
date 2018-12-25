package bright;

import bright.Compiler.Error;
import bright.Compiler.Interpreter;
import bright.Compiler.Pcode;
import bright.Compiler.RD_Analyzer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Test {

    public static void main(String[] args) {
        String filename = "C://Users//dsz62//Desktop//2018_Fall//" +
                "编译//Lab//Lab_1//PL0Compiler-master//testPL0//wrong1.pl0";
        File file  = new File(filename);
        RD_Analyzer compiler = new RD_Analyzer(file);
        boolean succeed = compiler.compile();

        System.out.println("===================Error==================");
        for (Error error : compiler.getErrors()){
            System.out.println(error);
        }
        System.out.println("===================Pcode==================");
        for(Pcode pcode : compiler.getPcodes()){
            System.out.println(pcode);
        }

        if(succeed){
            List<Integer> input = new ArrayList<>();
            input.add(35); input.add(94);
            Interpreter interpreter = new Interpreter(compiler.getPcodes(), input);
            interpreter.interpret();
            System.out.println("===================Output==================");
            for(String output : interpreter.getOutput()){
                System.out.println(output);
            }
        }
    }
}
