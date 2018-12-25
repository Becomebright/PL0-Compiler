package bright;

import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.util.ArrayList;
import java.util.List;

public class parseIntTest {
    public static void main(String[] args) {
        String s = "93 16 a b\r\n76  123";
        for(int i : getInput(s)){
            System.out.println(i);
        }
    }
    private static List<Integer> getInput(String s){
        s = s.replaceAll("\r", " ");
        s = s.replaceAll("\n", " ");
        List<Integer> ret = new ArrayList<>();
        String[] nums = s.split(" ");
        for(String num : nums){
            if(num.equals("")) continue;
            try {
                ret.add(Integer.parseInt(num));
            }catch (NumberFormatException ignored){
            }
        }
        return ret;
    }
}
