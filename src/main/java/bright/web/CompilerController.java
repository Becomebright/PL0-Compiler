package bright.web;

import bright.Compiler.Error;
import bright.Compiler.Interpreter;
import bright.Compiler.Pcode;
import bright.Compiler.RD_Analyzer;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class Result{
    private List<Pcode> pcodeList;
    private List<Error> errorList;
    private List<String> outputList;

    public Result(List<Pcode> pcodeList, List<Error> errorList) {
        this.pcodeList = pcodeList;
        this.errorList = errorList;
    }
    public Result(List<Pcode> pcodeList, List<Error> errorList, List<String> outputList) {
        this.pcodeList = pcodeList;
        this.errorList = errorList;
        this.outputList = outputList;
    }

    public List<Pcode> getPcodeList() {
        return pcodeList;
    }
    public List<Error> getErrorList() {
        return errorList;
    }
    public List<String> getOutputList() {
        return outputList;
    }
}

@Controller
public class CompilerController {

    @RequestMapping(value = "/compiler", method = RequestMethod.GET)
    public String ui(){
        return "Compiler";
    }

    // 编译
    @ResponseBody
    @RequestMapping(value = "/compiler/compile", method = RequestMethod.POST)
    public String compile(@RequestBody JSONObject jsonParam) throws IOException {
        //System.out.println(jsonParam);

        String code = jsonParam.getString("code").replaceAll("\r", "");
        Random random = new Random();
        File file = new File("upload-dir/code" + random.nextInt(10000) + ".txt");
        FileWriter writer = new FileWriter(file);
        writer.write(code);
        writer.close();

        RD_Analyzer compiler = new RD_Analyzer(file);
        try {
            compiler.compile();
        }catch (Exception e){
            List<String> error = new ArrayList<>();
            error.add("Compile Error!");
            Result compileResult = new Result(compiler.getPcodes(), compiler.getErrors(), error);
            return JSON.toJSONString(compileResult);
        }

        Result compileResult = new Result(
                compiler.getPcodes(), compiler.getErrors());
        return JSON.toJSONString(compileResult);
    }

    // 编译并执行
    @ResponseBody
    @RequestMapping(value = "/compiler/interpret", method = RequestMethod.POST)
    public String interpret(@RequestBody JSONObject jsonParam) throws IOException {
        String code = jsonParam.getString("code").replaceAll("\r", "");
        List<Integer> inputList = getInput(jsonParam.getString("input"));

        Random random = new Random();
        File file = new File("upload-dir/code" + random.nextInt(10000) + ".txt");
        FileWriter writer = new FileWriter(file);
        writer.write(code);
        writer.close();

        RD_Analyzer compiler = new RD_Analyzer(file);
        compiler.compile();

        Interpreter interpreter = new Interpreter(compiler.getPcodes(), inputList);
        try {
            interpreter.interpret();
        }catch (Exception e){
            List<String> error = new ArrayList<>();
            error.add("Runtime Error!");
            Result interpretResult = new Result(compiler.getPcodes(), compiler.getErrors(), error);
            return JSON.toJSONString(interpretResult);
        }
        Result interpretResult = new Result(compiler.getPcodes(), compiler.getErrors(), interpreter.getOutput());
        return JSON.toJSONString(interpretResult);
    }

    private List<Integer> getInput(String s){
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
