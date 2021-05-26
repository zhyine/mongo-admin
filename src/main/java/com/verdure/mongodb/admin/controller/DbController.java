package com.verdure.mongodb.admin.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.DBCursor;
import com.mongodb.client.*;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.verdure.mongodb.admin.config.MongoSDK;
import com.verdure.mongodb.admin.utils.ByteConvKbUtils;
import com.verdure.mongodb.admin.utils.JsonFormatTool;
import com.verdure.mongodb.admin.utils.Response;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;

import static com.mongodb.client.model.Aggregates.match;
import static com.verdure.mongodb.admin.config.MongoSDK.diyObjectIdToJson;
import static com.verdure.mongodb.admin.utils.Constant.CACHE;
import static com.verdure.mongodb.admin.utils.Constant.TAVLEARR;

/**
 * @author verdure
 * @date 2021/5/13 9:39 下午
 */
@Controller
@RequestMapping(value = "/db")
public class DbController {

    @Autowired
    public MongoSDK mongoSDK;

    @ModelAttribute
    public List<String> getDBs(Model model) {
        List<String> dbNames = mongoSDK.getDBList();
        // 系统表过滤
        dbNames.remove("admin");
        dbNames.remove("local");
        System.out.println(dbNames);
        model.addAttribute("dbNames", dbNames);
        model.addAttribute("cache", CACHE);
        System.out.println(CACHE);
        return dbNames;
    }

    /**
     * 查询数据库所有集合
     *
     * @return
     */
    @GetMapping(value = "/{dbName}")
    public String showDBCollections(@PathVariable String dbName, Model model) {
        // 选中数据库
        MongoDatabase database = mongoSDK.useDB(dbName);
        // 列出数据库的所有集合
        MongoIterable<String> mongoIterable = database.listCollectionNames();
        MongoCursor<String> cursor = mongoIterable.iterator();
        List<JSONObject> list = new ArrayList<>();
        // 遍历迭代器
        while (cursor.hasNext()) {
            String tableName = cursor.next();
            // 过滤系统表
            if (!Arrays.asList(TAVLEARR).contains(tableName)) {
                JSONObject object = new JSONObject();
                object.put("tableName", tableName);
                //                System.out.println(tableName);
                BasicDBObject basicDBObject = mongoSDK.getStatus(dbName, tableName);
                object.put("size", ByteConvKbUtils.getPrintSize(basicDBObject.getInt("size")));
                //                System.out.println(basicDBObject);
                list.add(object);
            }
        }
//        System.out.println(list);
        model.addAttribute("collections", list);
        return "db";
    }

    /**
     * 获取集合信息
     * 暂未使用
     *
     * @param dbName
     * @return
     */
    @ResponseBody
    @GetMapping(value = "/{dbName}/getInfo")
    public JSONObject getCollectionsInfoByPage(@PathVariable String dbName, HttpServletRequest request) {
        JSONObject jsonObject = null;
        // 选中数据库
        MongoDatabase database = mongoSDK.useDB(dbName);
        // 列出数据库的所有集合
        MongoIterable<String> mongoIterable = database.listCollectionNames();
        MongoCursor<String> cursor = mongoIterable.iterator();
        List<JSONObject> list = new ArrayList<>();
        // 遍历迭代器
        while (cursor.hasNext()) {
            String tableName = cursor.next();
            // 过滤系统表
            if (!Arrays.asList(TAVLEARR).contains(tableName)) {
                JSONObject object = new JSONObject();
                object.put("tableName", tableName);
                //                System.out.println(tableName);
                BasicDBObject basicDBObject = mongoSDK.getStatus(dbName, tableName);
                object.put("size", ByteConvKbUtils.getPrintSize(basicDBObject.getInt("size")));
                //                System.out.println(basicDBObject);
                list.add(object);
            }
        }
//        System.out.println(list);
        jsonObject.put("data", list);
        return jsonObject;
    }

    /**
     * 返回文档页面
     *
     * @return
     */
    @GetMapping(value = "/{dbName}/{collectionName}")
    public String showDocumentsByCollentions() {
        return "documents";
    }

    /**
     * 删除集合
     * @param dbName
     * @param collectionName
     * @return
     */
    @GetMapping(value = "/{dbName}/{collectionName}/drop")
    public Response dropCollection(
            @PathVariable String dbName,
            @PathVariable String collectionName) {
        mongoSDK.dropOne(mongoSDK.getDocuments(dbName, collectionName));
        return Response.OK("删除集合成功");
    }

    /**
     * 获取文档页面的分页信息
     *
     * @param dbName
     * @param collectionName
     * @param request
     * @return
     */
    @ResponseBody
    @GetMapping(value = "/{dbName}/{collectionName}/getInfo")
    public JSONObject getDocumentInfoByPage(
            @PathVariable String dbName,
            @PathVariable String collectionName,
            HttpServletRequest request) {

        String strPage = request.getParameter("page");
        String strStart = request.getParameter("start");
        String strLength = request.getParameter("length");

        int page = strPage == null ? 0 : Integer.parseInt(strPage);
        int start = strStart == null ? 0 : Integer.parseInt(strStart);
        int length = strLength == null ? 0 : Integer.parseInt(strLength);


        MongoCollection<Document> collection = mongoSDK.getDocuments(dbName, collectionName);
        // System.out.println(collection);
        JSONObject documentsInfo = null;

        documentsInfo = mongoSDK.getDocumentsByPage(collection, Sorts.descending("_id"), page, length);

//        System.out.println(documentsInfo);
        return documentsInfo;
    }

    /**
     * 根据ID查询文档
     *
     * @param dbName
     * @param collectionName
     * @param id
     * @return
     */
    @ResponseBody
    @GetMapping("/{dbName}/{collectionName}/{id}/detail")
    public Response findOne(
            @PathVariable String dbName,
            @PathVariable String collectionName,
            @PathVariable String id) {
        System.out.println("id："+id);

        String result = mongoSDK.selectOne(mongoSDK.getDocuments(dbName, collectionName), id);

        System.out.println(result);
        System.out.println(JSONObject.parseObject(result));
        return Response.OK().put("data", JSONObject.parseObject(result));
    }

    /**
     * 更新文档
     *
     * @param dbName
     * @param collectionName
     * @param params
     * @return
     */
    @ResponseBody
    @PostMapping("/{dbName}/{collectionName}/update")
    public Response updateCollection(
            @PathVariable String dbName,
            @PathVariable String collectionName,
            String params) {

//        System.out.println(params);

        JSONObject info = JSONObject.parseObject(params);
//        System.out.println(info);
        String id = info.getString("_id");

        boolean flag = mongoSDK.updateOne(mongoSDK.getDocuments(dbName, collectionName), id, info);

        return flag == true ? Response.OK(200, "更新成功") : Response.ERR("更新失败");
    }

    /**
     * 插入文档
     *
     * @param dbName
     * @param collectionName
     * @param params
     * @return
     */
    @ResponseBody
    @PostMapping(value = "/{dbName}/{collectionName}/insert")
    public Response insertDocument(
            @PathVariable String dbName,
            @PathVariable String collectionName,
            String params
    ) {
//        System.out.println(params);

        JSONObject info = JSONObject.parseObject(params);
//        System.out.println(info);
        String id = mongoSDK.insertOne(mongoSDK.getDocuments(dbName, collectionName), info);

        return !StringUtils.isEmpty(id) ? Response.OK(200, "插入成功") : Response.ERR("插入失败");

    }

    /**
     * 删除集合内的对应文档
     *
     * @param dbName
     * @param collectionName
     * @param id
     * @return
     */
    @ResponseBody
    @GetMapping(value = "/{dbName}/{collectionName}/{id}/delete")
    public Response delectDocumentById(
            @PathVariable String dbName,
            @PathVariable String collectionName,
            @PathVariable String id
    ) {
        // 获取对应的集合
        MongoCollection<Document> mongoCollection = mongoSDK.getDocuments(dbName, collectionName);
        // 删除集合内_id为对应值的一条文档
        int count = mongoSDK.deleteOne(mongoCollection, id);

//        System.out.println(count);
        return count > 0 ? Response.OK(200, "删除成功") : Response.ERR(200, "删除失败");
    }

    /**
     * 导出数据
     *
     * @param request
     * @param response
     * @param dbName
     * @param collectionName
     */
    @RequestMapping("/{dbName}/{collectionName}/exportList")
    public void exportList(HttpServletRequest request,
                           HttpServletResponse response,
                           @PathVariable String dbName,
                           @PathVariable String collectionName) {
        // 读取字符编码
        String csvEncoding = "UTF-8";
        // 设置响应
        response.setCharacterEncoding(csvEncoding);
        response.setContentType("application/json; charset=" + csvEncoding);
        response.setHeader("Pragma", "public");
        response.setHeader("Cache-Control", "max-age=30");
        OutputStream outputStream = null;
        try {
            BasicDBObject query = new BasicDBObject();
            List<JSONObject> list = mongoSDK.getAll(mongoSDK.getDocuments(dbName, collectionName), query, Sorts.descending("_id"));

            response.setHeader("Content-Disposition", "attachment; filename=\"" + collectionName + ".json" + "\"");
            // 写出响应
            outputStream = response.getOutputStream();

            outputStream.write(JsonFormatTool.formatJson(list.toString()).getBytes("GBK"));
            outputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 执行命令
     * @param dbName
     * @param collectionName
     * @param request
     * @param params
     * @return
     */
    @ResponseBody
    @PostMapping( value = "/{dbName}/{collectionName}/search")
    public Response searchByParams(
            @PathVariable String dbName,
            @PathVariable String collectionName,
            HttpServletRequest request,
            String params
    ) {
        String strPage = request.getParameter("page");
        String strStart = request.getParameter("start");
        String strLength = request.getParameter("length");

        int page = strPage == null ? 0 : Integer.parseInt(strPage);
        int start = strStart == null ? 0 : Integer.parseInt(strStart);
        int length = strLength == null ? 0 : Integer.parseInt(strLength);

        BasicDBObject query = new BasicDBObject();
        BasicDBObject group = new BasicDBObject();

        if(!StringUtils.isEmpty(params)){
            JSONObject jsonObject = JSONObject.parseObject(params);
            Set<String> kyes = jsonObject.keySet();
            kyes.forEach(key->{
                if(key.equals("$group")){
                    group.put(key,jsonObject.get(key));
                }else {
                    query.put(key, jsonObject.get(key));
                }
            });
        }

        MongoCollection<Document> collection = mongoSDK.getDocuments(dbName, collectionName);
        JSONObject jsonObject = null;
        jsonObject = mongoSDK.getDocumentsByGroupAndPage(collection, query, group, Sorts.descending("_id"), page, length);
        return Response.OK("查询成功");
    }

    /**
     * 命令查询
     * @param dbName
     * @param collectionName
     * @param params
     * @return
     */
    @ResponseBody
    @PostMapping( value = "/{dbName}/{collectionName}/code")
    public Response runCommand(
            @PathVariable String dbName,
            @PathVariable String collectionName,
            String params) {

        BasicDBObject findParams = new BasicDBObject();
        BasicDBObject sortParams = new BasicDBObject();
        BasicDBObject groupParams = new BasicDBObject();
        BasicDBObject filterParams = new BasicDBObject();

        if(!StringUtils.isEmpty(params)) {
            JSONObject jsonObject = JSONObject.parseObject(params);
//            System.out.println(jsonObject);
            Set<String> kyes = jsonObject.keySet();
            kyes.forEach(key->{
                if(key.equals("$find")){
                    System.out.println("find操作");
                    // 处理查询参数
                    String findString = jsonObject.get(key).toString();
//                    System.out.println(findString);
                    String[] findParamsArray = jsonObject.get(key).toString().substring(1, findString.length()-1).split(",");
                    for(String cur : findParamsArray) {
                        String[] strings = cur.split(":");
//                        System.out.println(strings[0].replace("\"", "")+strings[1]);
                        findParams.put(strings[0].replace("\"", ""), StringUtils.isNumeric(strings[1].replace("\"", "")) ? Integer.parseInt(strings[1].replace("\"", "")) : strings[1].replace("\"", ""));
                    }
                } else if(key.equals("$sort")){
                    System.out.println("sort操作");
                    String sortString = jsonObject.get(key).toString();
                    String[] sortParamsArray = jsonObject.get(key).toString().substring(1, sortString.length()-1).split(",");
                    for(String cur : sortParamsArray) {
                        String[] strings = cur.split(":");
//                        System.out.println(strings[0].replace("\"", "")+strings[1]);
                        sortParams.put(strings[0].replace("\"", ""), StringUtils.isNumeric(strings[1].replace("\"", "")) ? Integer.parseInt(strings[1].replace("\"", "")) : strings[1].replace("\"", ""));
                    }
                } else if(key.equals("$match")) {
                    System.out.println("match操作");
                    String filterString = jsonObject.get(key).toString();
                    System.out.println(filterString);
                    String[] filterParamsArray = jsonObject.get(key).toString().substring(1, filterString.length()-1).split(",");
                    for(String cur : filterParamsArray) {
                        String[] strings = cur.split(":");
//                        System.out.println(strings[0].replace("\"", "")+strings[1]);
                        filterParams.put(strings[0].replace("\"", ""), StringUtils.isNumeric(strings[1].replace("\"", "")) ? Integer.parseInt(strings[1].replace("\"", "")) : strings[1].replace("\"", ""));
                    }
                } else if(key.equals("$group")) {
                    System.out.println("group操作");
                    String groupString = jsonObject.get(key).toString();
                    String[] groupParamsArray = jsonObject.get(key).toString().substring(1, groupString.length()-1).split(",");
                    for(String cur : groupParamsArray) {
                        String[] strings = cur.split(":");
//                        System.out.println(strings[0].replace("\"", "")+strings[1]);
                        groupParams.put(strings[0].replace("\"", ""), StringUtils.isNumeric(strings[1].replace("\"", "")) ? Integer.parseInt(strings[1].replace("\"", "")) : strings[1].replace("\"", ""));
                    }
                }
            });
        }

        System.out.println("findParams:"+findParams);
        System.out.println("sortParams:"+sortParams);
        System.out.println("groupParams:"+groupParams);
        System.out.println("filterParams:"+filterParams);


        if(sortParams.isEmpty()) sortParams.put("_id", 1);

        MongoCollection<Document> collection = mongoSDK.getDocuments(dbName, collectionName);

        MongoCursor<Document> mongoCursor = null;

        if(filterParams.size() == 0 && groupParams.size() == 0) {
            mongoCursor = collection.find(findParams).sort(sortParams).iterator();
        } else {
            System.out.println("进入Aggregate");
            mongoCursor = collection.aggregate(Arrays.asList(
                    Aggregates.match(Filters.eq("categories", "Bakery")),
                    Aggregates.group("$stars", Accumulators.sum("count", 1))
            )).iterator();
        }

        List<JSONObject> list = new ArrayList<>();
        while (mongoCursor.hasNext()) {
            Document document = mongoCursor.next();
            list.add(JSONObject.parseObject(diyObjectIdToJson(document)));
        }

        return Response.OK(200, "执行成功").put("data", list);
    }




}