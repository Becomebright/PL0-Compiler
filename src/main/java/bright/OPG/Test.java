package bright.OPG;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Test {
    public static void main(String[] args) {
//        List<String> grammars = new ArrayList<>();
//        grammars.add("E->E+T|T");
//        grammars.add("T->T*F|F");
//        grammars.add("F->(E)|i");
//        String []sl= {
//                "i+i*i+i#"
//        };
//        OPG_Analyzer opg = new OPG_Analyzer(grammars);
//        for(String s : sl){
//            int result = opg.analyze(s);
//            if(result == 1)
//                System.out.println("规约成功");
//            else
//                System.out.println("规约失败");
//        }

//        Map<Character, Set<Character>> first = new HashMap<>();
//        first.put('U', new HashSet<>());
//
//        Map<Pair<Character, Character>, Boolean> F = new HashMap<>();
//        F.put(new Pair<>('U', 'b'), true);
//
//        func(first, F);
//        System.out.println(first.get('U'));
    }

    private static void func(Map<Character, Set<Character>> first,
                             Map<Pair<Character, Character>, Boolean> F){
        first.get('U').add('b');
        System.out.println(F.get(new Pair<>('U', 'b')));
    }
}
