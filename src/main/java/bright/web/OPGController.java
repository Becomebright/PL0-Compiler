package bright.web;

import bright.OPG.OPG_Analyzer;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.BufferedReader;
import java.util.*;

@Controller
public class OPGController {

    @Autowired
    public OPGController() {

    }

    @RequestMapping(value = "/opg", method = RequestMethod.GET)
    public String ui(Model model){
        return "OPG";
    }

    @ResponseBody
    @RequestMapping(value = "/opg/analyze", method = RequestMethod.POST)
    public Object analyze(@RequestBody JSONObject jsonParam){
        String []grammarString = jsonParam.getString("grammar").replaceAll("\r", "").split("\n");
        List<String> grammar = new ArrayList<>(Arrays.asList(grammarString));
        String sentence = jsonParam.getString("sentence");
        OPG_Analyzer.AnalyzeResult result;

        OPG_Analyzer analyzer = new OPG_Analyzer(grammar);
        analyzer.analyze(sentence);
        result = analyzer.result;
        System.out.println(result.isError());
        //System.out.println(JSON.toJSONString(result));

        return JSON.toJSONString(result);
    }
}
