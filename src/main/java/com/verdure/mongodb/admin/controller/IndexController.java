package com.verdure.mongodb.admin.controller;

import com.alibaba.fastjson.JSONObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.verdure.mongodb.admin.config.MongoSDK;
import com.verdure.mongodb.admin.utils.Response;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.verdure.mongodb.admin.utils.Constant.*;

/**
 * @author verdure
 * @date 2021/5/9 下午4:54
 */


@Controller
@RequestMapping("/oauthed")
public class IndexController {
    
    @Autowired
    public MongoSDK mongoSDK;

    @ModelAttribute
    public List<String> getDBs(Model model) {
        List<String> dbNames = mongoSDK.getDBList();
        // 系统表过滤
        dbNames.remove("admin");
        dbNames.remove("local");
//        System.out.println(dbNames);

        Map<String, List<String>> mapName = new HashMap<>();


        for(String dbName : dbNames) {
            List<String> gridFSFiles = mongoSDK.getAllGridFSNameByDBName(dbName);

//            System.out.println(gridFSFiles);
            if(gridFSFiles.size() != 0){
                mapName.put(dbName, gridFSFiles);
            }
        }

        System.out.println(mapName);

        model.addAttribute("dbNames", dbNames);

        model.addAttribute("files", mapName);

         model.addAttribute("cache", CACHE);
        // System.out.println(CACHE);
        return dbNames;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }



    @GetMapping(value = "/default")
    public String setDefault() {

        mongoSDK.mongoClient = HOSTOMONGOCLIENT.get("127.0.0.1");
        CACHE.clear();
        if(CACHE.isEmpty()) CACHE.add("127.0.0.1");

        return "redirect:/oauthed/";
    }

    /**
     * 更换MongoDB数据库连接
     * @param request
     * @return
     */
    @ResponseBody
    @PostMapping(value = "/addServer")
    public Response addServer(
            HttpServletRequest request
    ) {
        String host = request.getParameter("host");
        String port = request.getParameter("port");
        String name = request.getParameter("name");
        String auth = request.getParameter("auth");
        if(StringUtils.isEmpty(host) || StringUtils.isEmpty(port)) {
            return Response.ERR(500,"请填写建立连接的host, port, name以及auth");
        }

        if(!CACHE.contains(host)){
            if(!CACHE.isEmpty()) {
                CACHE.clear();
            }
            CACHE.add(host);
        }
        if(!CONNECTIONSCACHE.containsKey(host)) {
            Map<String, String> map = new HashMap<>();
            map.put("host", host);
            map.put("port", port);
            map.put("name", name);
            map.put("auth", auth);
            CONNECTIONSCACHE.put(host, map);
        }

        MongoClient mongoClient = null;
        List<MongoCredential> mongoCredentials = Collections.emptyList();
        MongoCredential mongoCredential = MongoCredential.createScramSha1Credential(name, "admin", auth.toCharArray())
                ;
        mongoCredentials = new ArrayList<MongoCredential>();
        mongoCredentials.add(mongoCredential);
        // 其他选项
        MongoClientOptions options = MongoClientOptions.builder().build();

        mongoClient = new MongoClient(new ServerAddress(host, Integer.parseInt(port)), mongoCredentials, options);

        HOSTOMONGOCLIENT.put(host, mongoClient);
        
        return HOSTOMONGOCLIENT.containsKey(host) ? Response.OK(200,"添加连接成功") : Response.ERR(500, "添加连接失败");
    }

    /**
     * 更换数据库连接
     * @param host
     * @return
     */
    @GetMapping(value = "/change/{host}")
    public String changeClient(@PathVariable String host) {
        System.out.println("重新连接数据库"+host);

        mongoSDK.mongoClient = HOSTOMONGOCLIENT.get(host);

        return "redirect:/";
    }

}