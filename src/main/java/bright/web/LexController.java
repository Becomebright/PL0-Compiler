package bright.web;

import bright.Lex.LexAnalyzer;
import bright.Lex.SymType;
import bright.Lex.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class LexController {

    private final StorageService storageService;

    @Autowired
    public LexController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping({"/", "/lexer"})
    public String ui(Model model){
        return "ui2";
    }

    //处理文件上传
    @PostMapping({"/", "/lexer"})
    //@ResponseBody
    public String upload(@RequestParam("txt_file") MultipartFile file,
            RedirectAttributes redirectAttributes) throws IOException {
        storageService.store(file);  // 保存文件
        String filename = "upload-dir/" + StringUtils.cleanPath(file.getOriginalFilename());  // 保存位置

        File file1 = new File(filename);// 读取文件内容
        BufferedReader bf = new BufferedReader(new FileReader(file1));
        String s1;
        StringBuilder code_text = new StringBuilder();
        while((s1 = bf.readLine()) != null)
            code_text.append(s1).append(String.valueOf('\n'));
        bf.close();

        List<String> errors = new ArrayList<>();
        List<Map<String, Object>> tokens = getTokens(filename, errors);  // 词法分析结果
//        StringBuilder errorMessage = new StringBuilder();
//        for(int i=0; i<errors.size() && i<5; i++)
//            errorMessage.append(errors.get(i)).append('\n');
        if (errors.size()>0){
            errors = errors.subList(0,Math.min(4,errors.size()));  // 最多同时显示4条错误
            redirectAttributes.addFlashAttribute("errors", errors);  // 分析错误
        }
        else{
            redirectAttributes.addFlashAttribute("success",
                    "Analysis completed. 0 errors, 0 warnings.");
        }

        redirectAttributes.addFlashAttribute("message",  // 向前端传参
                "Upload successfully to: " + filename);
        redirectAttributes.addFlashAttribute("code", code_text);
        redirectAttributes.addFlashAttribute("tokens", tokens);

        return "redirect:/";
    }

    /**
     * @param filePath 文件名
     * @return token的json列表
     */
     private List<Map<String, Object>> getTokens(String filePath, List<String> errors){
        File file = new File(filePath);
        LexAnalyzer la = new LexAnalyzer(file);
        List<Token> tokenList = la.getTokens();

        List<Map<String, Object>> res = new ArrayList<>();
        Map<String, Object> token_json;
        for(Token token : tokenList){
            token_json = new HashMap<>();
            token_json.put("token", token.getToken());
            token_json.put("type", token.getType().getTypeName());
            if(token.getType()== SymType.INT || token.getType()== SymType.REAL)
                token_json.put("value", token.getValue());
            else if(token.getType() == SymType.ERROR){
                errors.add(token.getValue());
                continue;
            }
            else if(token.getType() == SymType.EOF)
                continue;
            else
                token_json.put("value", token.getToken());
            res.add(token_json);
            //res.append(i++).append("\t").append(token.getType().getType()).append(" \t").append(token.getType()).append(" \t").append(token.getValue()).append('\n');
            //System.out.println(i++ + "\t" + token.getType().getType() + " \t" + token.getType() + " \t" + token.getValue());
        }
        return res;
    }
}
