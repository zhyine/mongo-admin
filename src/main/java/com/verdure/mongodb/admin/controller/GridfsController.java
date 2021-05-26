package com.verdure.mongodb.admin.controller;

import com.alibaba.fastjson.JSONObject;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.model.Sorts;
import com.verdure.mongodb.admin.config.MongoSDK;
import com.verdure.mongodb.admin.utils.JsonFormatTool;
import com.verdure.mongodb.admin.utils.Response;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author verdure
 * @date 2021/5/25 7:19 下午
 */

@Controller
@RequestMapping("/GridFs")
public class GridfsController {

    @Autowired
    private MongoSDK mongoSDK;

    /**
     * 删除GridFS
     * @param dbName
     * @param id
     * @return
     */
    @GetMapping("/delete/{dbName}/{id}")
    public String deleteGridFs(
            @PathVariable String dbName,
            @PathVariable String id
    ) {
        System.out.println(id.replace("...", ""));
         mongoSDK.deleteGridFSByID(dbName, id.replace("...", ""));

         return "redirect:/";
    }

    /**
     * 更改GridFS名称
     * @param dbName
     * @param id
     * @return
     */
    @GetMapping("/update/{dbName}/{id}")
    public String updateGridFsName(
            @PathVariable String dbName,
            @PathVariable String id
    ) {
        System.out.println(id.replace("...", ""));
        mongoSDK.updateGridFSName(dbName, id.replace("...", ""));

        return "redirect:/";
    }

    /**
     * 下载文件
     * @param dbName
     * @param id
     * @return
     */
    @GetMapping("/download/{dbName}/{id}")
    public void downloadGridFs(
            @PathVariable String dbName,
            @PathVariable String id,
            HttpServletResponse response
    ) throws IOException {
        id = id.replace("...", "");

        GridFSBucket gridFSBucket = mongoSDK.downloadGridFS(dbName, id);
        ObjectId objectId = new ObjectId(id);

        OutputStream outputStream = null;

        GridFSDownloadStream downloadStream = gridFSBucket.openDownloadStream(objectId);
//        int fileLength = (int) downloadStream.getGridFSFile().getLength();
//        byte[] bytesToWriteTo = new byte[fileLength];
//        downloadStream.read(bytesToWriteTo);
//        downloadStream.close();

        response.setHeader("Content-Disposition", "attachment; filename=\"" + id + ".mp4" + "\"");
        // 写出响应
        outputStream = response.getOutputStream();

        // 执行下载
        gridFSBucket.downloadToStream(objectId, outputStream);

    }



}