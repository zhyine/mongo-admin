package com.verdure.mongodb.admin.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializeFilter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.ValueFilter;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.eq;

/**
 * @author verdure
 * @date 2021/5/9 下午5:02
 */
@Component
public class MongoSDK {

    @Autowired
    public MongoClient mongoClient;

    /**
     * 获取所有数据库名
     *
     * @return
     */
    public List<String> getDBList() {
        return mongoClient.getDatabaseNames();
    }

    /**
     * 选中对应的Mongodb数据库
     *
     * @param dbName
     * @return
     */
    public MongoDatabase useDB(String dbName) {
        return mongoClient.getDatabase(dbName);
    }

    /**
     * 查询所有的gridfs文件名称
     * @param dbNames
     * @return
     */
    public List<String> getAllGridFSNameByDBName(String dbNames) {
        List<String> listName = new ArrayList<>();
        JSONObject jsonObject = null;

        MongoDatabase database = mongoClient.getDatabase(dbNames);
        GridFSBucket gridFSBucket = GridFSBuckets.create(database);
        gridFSBucket.find().forEach((Consumer<GridFSFile>) gridFSFile -> listName.add(gridFSFile.getObjectId()+"\n"+gridFSFile.getFilename()+"\n"));

        return listName;
    }

    /**
     * 删除GridFS
     * @param dbName
     * @param id
     */
    public void deleteGridFSByID(String dbName, String id) {
        MongoDatabase database = mongoClient.getDatabase(dbName);
        GridFSBucket gridFSBucket = GridFSBuckets.create(database);
        ObjectId objectId = new ObjectId(id);
        gridFSBucket.delete(objectId);
    }

    /**
     * 更新GridFS名称
     * @param dbName
     * @param id
     */
    public void updateGridFSName(String dbName, String id) {
        MongoDatabase database = mongoClient.getDatabase(dbName);
        GridFSBucket gridFSBucket = GridFSBuckets.create(database);
        ObjectId objectId = new ObjectId(id);
        gridFSBucket.rename(objectId, "a");
    }

    /**
     * 下载GridFS
     * @param dbName
     * @param id
     */
    public GridFSBucket downloadGridFS(String dbName, String id) {
        MongoDatabase database = mongoClient.getDatabase(dbName);
        GridFSBucket gridFSBucket = GridFSBuckets.create(database);

        return gridFSBucket;
    }

    /**
     * 获取表的详细信息
     *
     * @param dbName
     * @param collectionName
     * @return
     */
    public BasicDBObject getStatus(String dbName, String collectionName) {
        DB db = new DB(mongoClient, dbName);
        return db.getCollection(collectionName).getStats();
    }

    /**
     * 获取集合内的所有文档
     *
     * @param dbName
     * @param collectionName
     * @return
     */
    public MongoCollection<Document> getDocuments(String dbName, String collectionName) {
        MongoDatabase db = mongoClient.getDatabase(dbName);
        return db.getCollection(collectionName);
    }

    /**
     * 对集合内的文档进行分页查询
     *
     * @param collection
     * @param sort
     * @param pageNum
     * @param pageSize
     * @return
     */
    public JSONObject getDocumentsByPage(MongoCollection collection, Bson sort, int pageNum, int pageSize) {
        // 获取集合内的所有文档
        int documentsCount = (int) collection.countDocuments();
        int pages = (documentsCount / pageSize) + ((documentsCount % pageSize == 0) ? 0 : 1);
        if (pageNum > pages) pageNum = pages;

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("draw", pageNum);
        jsonObject.put("length", pageSize);
        jsonObject.put("total", documentsCount);
        jsonObject.put("pages", pages);

        List<JSONObject> list = new ArrayList<>();
        if (documentsCount > 0) {
            int start = pageNum > 0 ? (pageNum - 1) * pageSize : 0;
            FindIterable<Document> findIterable = null;

            findIterable = collection.find().sort(sort).skip(start).limit(pageSize);

            MongoCursor<Document> cursor = findIterable.iterator();
            while (cursor.hasNext()) {
                Document cur = cursor.next();
                list.add(JSONObject.parseObject(diyObjectIdToJson(cur)));
            }
        }
        jsonObject.put("data", list);
        return jsonObject;
    }

    /**
     * 删除集合
     * @param collection
     */
    public void dropOne(MongoCollection collection) {
//        System.out.println(collection);
        collection.drop();
    }

    /**
     * 分组分页查询
     * @param collection
     * @param filter
     * @param group
     * @param sort
     * @param pageNum
     * @param pageSize
     * @return
     */
    public JSONObject getDocumentsByGroupAndPage(MongoCollection collection,Bson filter, Bson group, Bson sort, int pageNum, int pageSize) {
        List<JSONObject> list = new ArrayList<>();
        int startRow = pageNum > 0 ? (pageNum - 1) * pageSize : 0;
        MongoCursor<Document> cursor = collection.aggregate(Arrays.asList(match(filter), group, sort(sort), skip(startRow), limit(pageSize))).iterator();
        while (cursor.hasNext()) {
            Document doc = cursor.next();
            list.add(JSON.parseObject(diyObjectIdToJson(doc)));
        }
        int documentsCount = list != null && list.size() > 0 ? list.size() : 0;
        int pages = documentsCount / pageSize + ((documentsCount % pageSize == 0) ? 0 : 1);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("pageNum", pageNum);
        jsonObject.put("pageSize", pageSize);
        jsonObject.put("total", documentsCount);
        jsonObject.put("pages", pages);
        jsonObject.put("data", list);
        return jsonObject;
    }

    /**
     * 删除找到的第一个文档
     * @param collection
     * @return
     */
    public  int deleteOne(MongoCollection collection, String id) {
        Bson filter = eq("_id", id);
        DeleteResult re = collection.deleteOne(filter);
        return (int) re.getDeletedCount();
    }

    /**
     * 根据id查询文档
     * @param collection
     * @param id
     * @return
     */
    public String selectOne(MongoCollection collection, String id) {
        Bson filter = eq("_id", new ObjectId(id));
        return diyObjectIdToJson(seleteOneDocument(collection, filter));
    }

    /**
     * 更新文档
     * @param collection
     * @param id
     * @param object
     * @return
     */
    public Boolean updateOne(MongoCollection collection, String id, Object object) {
        Bson filter = eq("_id", id);
        collection.updateOne(filter, set(diyObjectIdToJson(object)));
        return true;
    }

    /**
     * 插入文档
     * @param collection
     * @param info
     * @return
     */
    public String insertOne(MongoCollection collection, Object info) {
        if(info == null) return null;
        Document document = Document.parse(diyObjectIdToJson(info));
        document.remove("_id");
        document.put("_id", new ObjectId());
        collection.insertOne(document);
        return document.get("_id").toString();
    }

    /**
     * 查询所有记录数
     * @param collection
     * @param filter
     * @param sort
     * @return
     */
    public  List<JSONObject> getAll(MongoCollection collection, Bson filter, Bson sort) {
        List<JSONObject> list = new ArrayList<JSONObject>();
        FindIterable<Document> result = null;
        if (filter == null) {
            result = collection.find().sort(sort);
        } else {
            result = collection.find(filter).sort(sort);
        }

        MongoCursor<Document> iterator = result.iterator();

        while (iterator.hasNext()) {
            Object ddd = iterator.next();
            list.add(JSON.parseObject(diyObjectIdToJson(ddd)));
        }
        return list;
    }

    private static SerializeFilter objectIdSerializer = new ValueFilter() {
        @Override
        public Object process(Object object, String name, Object value) {
            if ("_id".equals(name)) {
                if (value instanceof ObjectId) {
                    return value.toString();
                }
            }
            return value;
        }
    };

    /**
     * 查询 单条记录 返回 org.bson.Document  对象
     * @param collection
     * @return
     */
    public  Document seleteOneDocument(MongoCollection collection, Bson filter) {
        FindIterable<Document> result = collection.find(filter);
        System.out.println("result："+result.first());
        return result.first();
    }

    /**
     * 更新数据
     * @param json
     * @return
     */
    public static Document set(String json) {
        Document document = Document.parse(json);
        document.remove("_id");
        return new Document("$set", document);
    }

    public static final String diyObjectIdToJson(Object object) {
        return JSON.toJSONString(object, objectIdSerializer,
                SerializerFeature.WriteDateUseDateFormat,
                SerializerFeature.WriteNullNumberAsZero,
                SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullStringAsEmpty);
    }


}