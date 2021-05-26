package com.verdure.mongodb.admin.config;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.verdure.mongodb.admin.utils.Constant.*;

@Component
public class MongoConfig {

    @Value("${mongo.host}")
    public String HOST;

    @Value("${mongo.port}")
    public String PORT;

    @Value("${mongo.user}")
    public String USER;

    @Value("${mongo.password}")
    public String PASSWORD;

    @Value("${mongo.db}")
    public String DB;

    /**
     * 连接MongoDB数据库
     * @return
     */
    @Bean
    public MongoClient mongoClient() {
        System.out.println("开始连接数据库");
        MongoClient mongoClient = null;

        // 连接Mongodb配置信息
        List<MongoCredential> mongoCredentials = Collections.emptyList();
        MongoCredential mongoCredential = MongoCredential.createScramSha1Credential(USER, DB, PASSWORD.toCharArray())
;
        mongoCredentials = new ArrayList<MongoCredential>();
        mongoCredentials.add(mongoCredential);
        // 其他选项
        MongoClientOptions options = MongoClientOptions.builder().build();

        String[] hosts = HOST.split(";");
        // 识别副本集
        if(hosts.length > 1) {
            List<ServerAddress> serverAddress = new ArrayList<ServerAddress>();
            String[] ports = HOST.split(";");
            String[] users = HOST.split(";");
            String[] passwords = HOST.split(";");
            String[] dbs = HOST.split(";");
            for(int i = 0; i < hosts.length; i++) {
                serverAddress.add(new ServerAddress(hosts[i], Integer.parseInt(ports[i])));
            }
            mongoClient = new MongoClient(serverAddress, mongoCredential,options);

        } else {
            mongoClient = new MongoClient(new ServerAddress(HOST, Integer.parseInt(PORT)), mongoCredentials, options);
        }

        CACHE.add(HOST);

        Map<String, String> map = new HashMap<>();
        map.put("host", HOST);
        map.put("port", PORT);
        map.put("name", USER);
        map.put("auth", PASSWORD);
        CONNECTIONSCACHE.put(HOST, map);

        HOSTOMONGOCLIENT.put(HOST, mongoClient);
        return mongoClient;
    }

}