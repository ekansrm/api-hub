package com.example.apihub.services.cloudbase;
import com.example.apihub.services.cloudbase.dto.*;
import com.example.apihub.utils.ParameterizedTypeBuilder;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class  CloudbaseDatabaseService<T> {

    private static final String ID_FIELD = "_id";

    private final String secretId;

    private final String secretKey;

    private final String envId;

    private final String collection;

    private Class<T> tClass;

    private final RestTemplate restTemplate;

    private final Gson gson;


    public CloudbaseDatabaseService(String secretId, String secretKey, String envId, String collection, Class<T> tClass) {
        this.secretId = secretId;
        this.secretKey = secretKey;
        this.envId = envId;
        this.collection = collection;
        this.tClass = tClass;
        this.restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        MappingJackson2HttpMessageConverter jsonHttpMessageConverter = new MappingJackson2HttpMessageConverter();
        jsonHttpMessageConverter.getObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        this.restTemplate.getMessageConverters().add(jsonHttpMessageConverter);

        this.gson = new GsonBuilder().create();
    }


    private T toDto(Document document) {
        return gson.fromJson(document.toJson(), tClass);
    }

    private Document toDoc(T dto) {
        String json = gson.toJson(dto);
        return Document.parse(json);
    }

    public T findById(String id) {
        CloudbaseDatabaseFindResult result = getDocument(collection, id, null);
        if(result.getData()!=null && result.getData().getList().size()>0) {
            Document document = Document.parse(result.getData().getList().get(0));
            return toDto(document);
        }
        return null;
    }

    public void saveById(String id, T data) {
        Document document = toDoc(data);
        document.put(ID_FIELD, id);
        setDocument(id, document);
    }

    public long count(Query query) {
        CloudbaseDatabaseCountResult result = countDocument(query);
        return result.getCount();
    }

    public List<T> find(Query query) {
        CloudbaseDatabaseFindResult findResult = findDocument(query);
        if(findResult.getData()!=null && findResult.getData().getList()!=null) {
            return findResult.getData().getList().stream()
                    .map(x-> toDto(Document.parse(x))).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public List<T> findAll() {
        Query query = new Query();
        return find(query);
    }

    public T insert(T data) {
        Document document = toDoc(data);
        if(document.containsKey(ID_FIELD)) {
            String id = document.getString(ID_FIELD);
            insertDocument(id, document);
        } else {
            CloudbaseDatabaseInsertResult result = insertDocuments(Collections.singletonList(document));
            if(result!=null && result.getData()!=null && result.getData().getInsertedIds()!=null && result.getData().getInsertedIds().size() >0) {
                document.put(ID_FIELD, result.getData().getInsertedIds().get(0));
            }
        }
        return toDto(document);
    }

    public List<T> insert(List<T> data) {
        List<Document> document = data.stream().map(this::toDoc).collect(Collectors.toList());

        List<Document> documentWithId = document.stream().filter(x->x.containsKey(ID_FIELD)).collect(Collectors.toList());
        List<Document> documentNoId = document.stream().filter(x->!x.containsKey(ID_FIELD)).collect(Collectors.toList());

        if(documentWithId.size()>0) {
            insertDocuments(documentWithId);
        }
        if(documentNoId.size()>0) {
            CloudbaseDatabaseInsertResult result = insertDocuments(documentNoId);
            for(int i=0; i<documentNoId.size(); i++) {
                documentNoId.get(i).put(ID_FIELD, result.getData().getInsertedIds().get(i));
            }
        }

        return document.stream().map(this::toDto).collect(Collectors.toList());

    }


    public UpdateResult updateFirst(Query query, Update update) {
        return updateOneDocument(query, update);
    }

    public UpdateResult updateMulti(Query query, Update update) {
        return updateManyDocument(query, update);
    }

    public DeleteResult remove(String id) {
        return deleteDocument(id);
    }

    public DeleteResult remove(Query query) {
        return deleteManyDocument(query);
    }

    public DeleteResult removeAll() {
        Query query = new Query();
        query.addCriteria(Criteria.where(ID_FIELD).exists(Boolean.TRUE));
        return deleteManyDocument(query);
    }


    public CloudbaseDatabaseFindResult getDocument(String id) {
        return getDocument(collection, id, null);
    }

    public CloudbaseDatabaseUpdateResult setDocument(String id, Document document) {

        return setDocument(collection, id, document, null);
    }

    public CloudbaseDatabaseInsertResult insertDocument(String id, Document document) {
        return insertDocument(collection, id, document, null);

    }

    public CloudbaseDatabaseInsertResult insertDocuments(List<Document> documents) {
        return insertDocuments(collection, documents, null);
    }

    public CloudbaseDatabaseUpdateResult updateDocument(String id, Document document) {
        return updateDocument(collection, id, document, null);

    }

    public CloudbaseDatabaseDeleteResult deleteDocument(String id) {

        return deleteDocument(collection, id, null);
    }

    public CloudbaseDatabaseFindResult findDocument(Query query) {
        return find(collection, query, null);
    }

    public CloudbaseDatabaseCountResult countDocument(Query query) {
        return count(collection, query, null);
    }


    public CloudbaseDatabaseUpdateResult updateOneDocument(Query query, Update update) {
        return updateOneDocument(collection, query, update, null);

    }

    public CloudbaseDatabaseUpdateResult updateManyDocument(Query query, Update update) {
        return updateManyDocument(collection, query, update, null);

    }

    public CloudbaseDatabaseDeleteResult deleteOneDocument(Query query) {
        return deleteOneDocument(collection, query, null);

    }

    public CloudbaseDatabaseDeleteResult deleteManyDocument(Query query) {
        return deleteManyDocument(collection, query, null);
    }
    private HttpHeaders buildHeaders() {

        return CloudbaseUtils.buildHeaders(secretId, secretKey);
    }

    private String addTransactionId(String url, String transactionId) {

        if(StringUtils.isNotBlank(transactionId)) {
            return url + "?transactionId=" + transactionId;
        }
        return url;
    }

    private CloudbaseDatabaseFindResult getDocument(String collection, String id, String transactionId) {

        //请使用此headers进行网络请求，访问相关接口
        //...相关网络请求代码
        HttpHeaders headers = buildHeaders();

        String url=addTransactionId("https://tcb-api.tencentcloudapi.com/api/v2/envs/{envId}/databases/{collectionName}/documents/{docId}", transactionId)
                .replace("{envId}", envId)
                .replace("{collectionName}", collection)
                .replace("{docId}", id);
        RequestEntity<Void> requestEntity = RequestEntity.get(url)
                .headers(headers)
                .accept(MediaType.APPLICATION_JSON)
                .build();

        ResponseEntity<CloudbaseDatabaseFindResult> response = restTemplate.exchange(requestEntity, CloudbaseDatabaseFindResult.class);

        return response.getBody();

    }

    private CloudbaseDatabaseUpdateResult setDocument(String collection, String id, Document document, String transactionId) {
        HttpHeaders headers = buildHeaders();
        Map<String, String> body = new HashMap<>();

        String url= addTransactionId("https://tcb-api.tencentcloudapi.com/api/v2/envs/{envId}/databases/{collectionName}/documents/{docId}", transactionId)
                .replace("{envId}", envId)
                .replace("{collectionName}", collection)
                .replace("{docId}", id);

        body.put("data", document.toJson());

        RequestEntity<Map<String, String>> requestEntity = RequestEntity.put(url)
                .headers(headers)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).body(body);


        ResponseEntity<CloudbaseDatabaseUpdateResult> response =
                restTemplate.exchange(requestEntity, CloudbaseDatabaseUpdateResult.class);

        return response.getBody();

    }

    private CloudbaseDatabaseInsertResult insertDocument(String collection, String id, Document document, String transactionId) {

        //请使用此headers进行网络请求，访问相关接口
        //...相关网络请求代码
        HttpHeaders headers = buildHeaders();

        Map<String, String> body = new HashMap<>();

        // 将 BSON 文档转成字符串
        body.put("data", document.toJson());

        String url=addTransactionId("https://tcb-api.tencentcloudapi.com/api/v2/envs/{envId}/databases/{collectionName}/documents/{docId}", transactionId)
                .replace("{envId}", envId)
                .replace("{collectionName}", collection)
                .replace("{docId}", id);
        RequestEntity<Map<String, String>> requestEntity = RequestEntity.post(url)
                .headers(headers)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).body(body)
                ;

        ResponseEntity<CloudbaseDatabaseInsertResult> response = restTemplate.exchange(requestEntity, CloudbaseDatabaseInsertResult.class);

        return response.getBody();

    }

    private CloudbaseDatabaseInsertResult insertDocuments(String collection, List<Document> documents, String transactionId) {

        //请使用此headers进行网络请求，访问相关接口
        //...相关网络请求代码
        HttpHeaders headers = buildHeaders();

        Map<String, Object> body = new HashMap<>();

        // 将 BSON 文档转成字符串
        List<String> data = documents.stream().map(Document::toJson).collect(Collectors.toList());
        body.put("data", data);

        String url=addTransactionId("https://tcb-api.tencentcloudapi.com/api/v2/envs/{envId}/databases/{collectionName}/documents", transactionId)
                .replace("{envId}", envId)
                .replace("{collectionName}", collection);

        RequestEntity<Map<String, Object>> requestEntity = RequestEntity.post(url)
                .headers(headers)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).body(body)
                ;

        ResponseEntity<CloudbaseDatabaseInsertResult> response = restTemplate.exchange(requestEntity, CloudbaseDatabaseInsertResult.class);

        return response.getBody();

    }

    private CloudbaseDatabaseUpdateResult updateDocument(String collection, String id, Document document, String transactionId) {

        HttpHeaders headers = buildHeaders();
        Map<String, String> body = new HashMap<>();


        String url=addTransactionId("https://tcb-api.tencentcloudapi.com/api/v2/envs/{envId}/databases/{collectionName}/documents/{docId}", transactionId)
                .replace("{envId}", envId)
                .replace("{collectionName}", collection)
                .replace("{docId}", id);

        // 将更新对象转成 BSON 文档
//        Document bsonDoc = update.getUpdateObject();

        // 将 BSON 文档转成字符串
        body.put("data", document.toJson());

        RequestEntity<Map<String, String>> requestEntity = RequestEntity.patch(url)
                .headers(headers)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).body(body);

        Type parameterizedTypeClass = new ParameterizedTypeBuilder(
                CloudbaseResponse.class, new Type[]{CloudbaseDatabaseUpdateResult.DataDTO.class}, CloudbaseResponse.class
        );

        ResponseEntity<CloudbaseDatabaseUpdateResult> response =
                restTemplate.exchange(requestEntity, CloudbaseDatabaseUpdateResult.class);

        return response.getBody();

    }

    private CloudbaseDatabaseDeleteResult deleteDocument(String collection, String id, String transactionId) {
        HttpHeaders headers = buildHeaders();

        String url=addTransactionId("https://tcb-api.tencentcloudapi.com/api/v2/envs/{envId}/databases/{collectionName}/documents/{docId}", transactionId)
                .replace("{envId}", envId)
                .replace("{collectionName}", collection)
                .replace("{docId}", id);

        RequestEntity<Void> requestEntity = RequestEntity.delete(url)
                .headers(headers)
                .accept(MediaType.APPLICATION_JSON)
                .build()
                ;

        ResponseEntity<CloudbaseDatabaseDeleteResult> response =
                restTemplate.exchange(requestEntity, CloudbaseDatabaseDeleteResult.class);

        return response.getBody();

    }

    private CloudbaseDatabaseFindResult find(String collection, Query query, String transactionId) {

        //请使用此headers进行网络请求，访问相关接口
        //...相关网络请求代码
        HttpHeaders headers = buildHeaders();
        Map<String, Object> body = new HashMap<>();

        body.put("query", query.getQueryObject().toJson());

        String url=addTransactionId("https://tcb-api.tencentcloudapi.com/api/v2/envs/{envId}/databases/{collectionName}/documents:find", transactionId)
                .replace("{envId}", envId)
                .replace("{collectionName}", collection)
                ;

        RequestEntity<Map<String, Object>> requestEntity = RequestEntity.post(url)
                .headers(headers)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).body(body);

        ResponseEntity<CloudbaseDatabaseFindResult> response = restTemplate.exchange(requestEntity, CloudbaseDatabaseFindResult.class);

        return response.getBody();

    }

    private CloudbaseDatabaseCountResult count(String collection, Query query, String transactionId) {

        //请使用此headers进行网络请求，访问相关接口
        //...相关网络请求代码
        HttpHeaders headers = buildHeaders();
        Map<String, Object> body = new HashMap<>();

        body.put("query", query.getQueryObject().toJson());

        String url=addTransactionId("https://tcb-api.tencentcloudapi.com/api/v2/envs/{envId}/databases/{collectionName}/documents:count", transactionId)
                .replace("{envId}", envId)
                .replace("{collectionName}", collection)
                ;

        RequestEntity<Map<String, Object>> requestEntity = RequestEntity.post(url)
                .headers(headers)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).body(body);

        ResponseEntity<CloudbaseDatabaseCountResult> response = restTemplate.exchange(requestEntity, CloudbaseDatabaseCountResult.class);

        return response.getBody();

    }

    private CloudbaseDatabaseUpdateResult updateOneDocument(String collection, Query query, Update update, String transactionId) {

        HttpHeaders headers = buildHeaders();
        Map<String, String> body = new HashMap<>();


        String url=addTransactionId("https://tcb-api.tencentcloudapi.com/api/v2/envs/{envId}/databases/{collectionName}/documents:updateOne", transactionId)
                .replace("{envId}", envId)
                .replace("{collectionName}", collection);

        // 将更新对象转成 BSON 文档
//        Document bsonDoc = update.getUpdateObject();

        // 将 BSON 文档转成字符串
        body.put("query", query.getQueryObject().toJson());
        body.put("data", update.getUpdateObject().toJson());

        RequestEntity<Map<String, String>> requestEntity = RequestEntity.post(url)
                .headers(headers)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).body(body);

        ResponseEntity<CloudbaseDatabaseUpdateResult> response =
                restTemplate.exchange(requestEntity, CloudbaseDatabaseUpdateResult.class);

        return response.getBody();

    }

    private CloudbaseDatabaseUpdateResult updateManyDocument(String collection, Query query, Update update, String transactionId) {

        HttpHeaders headers = buildHeaders();
        Map<String, String> body = new HashMap<>();


        String url=addTransactionId("https://tcb-api.tencentcloudapi.com/api/v2/envs/{envId}/databases/{collectionName}/documents:updateMany", transactionId)
                .replace("{envId}", envId)
                .replace("{collectionName}", collection);

        // 将更新对象转成 BSON 文档
//        Document bsonDoc = update.getUpdateObject();

        // 将 BSON 文档转成字符串
        body.put("query", query.getQueryObject().toJson());
        body.put("data", update.getUpdateObject().toJson());

        RequestEntity<Map<String, String>> requestEntity = RequestEntity.post(url)
                .headers(headers)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).body(body);

        ResponseEntity<CloudbaseDatabaseUpdateResult> response =
                restTemplate.exchange(requestEntity, CloudbaseDatabaseUpdateResult.class);

        return response.getBody();

    }

    private CloudbaseDatabaseDeleteResult deleteOneDocument(String collection, Query query, String transactionId) {

        HttpHeaders headers = buildHeaders();
        Map<String, String> body = new HashMap<>();


        String url=addTransactionId("https://tcb-api.tencentcloudapi.com/api/v2/envs/{envId}/databases/{collectionName}/documents:deleteOne", transactionId)
                .replace("{envId}", envId)
                .replace("{collectionName}", collection);

        // 将更新对象转成 BSON 文档
//        Document bsonDoc = update.getUpdateObject();

        // 将 BSON 文档转成字符串
        body.put("query", query.getQueryObject().toJson());

        RequestEntity<Map<String, String>> requestEntity = RequestEntity.post(url)
                .headers(headers)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).body(body);

        ResponseEntity<CloudbaseDatabaseDeleteResult> response =
                restTemplate.exchange(requestEntity, CloudbaseDatabaseDeleteResult.class);

        return response.getBody();

    }

    private CloudbaseDatabaseDeleteResult deleteManyDocument(String collection, Query query, String transactionId) {

        HttpHeaders headers = buildHeaders();
        Map<String, String> body = new HashMap<>();


        String url=addTransactionId("https://tcb-api.tencentcloudapi.com/api/v2/envs/{envId}/databases/{collectionName}/documents:deleteMany", transactionId)
                .replace("{envId}", envId)
                .replace("{collectionName}", collection);

        // 将更新对象转成 BSON 文档
//        Document bsonDoc = update.getUpdateObject();

        // 将 BSON 文档转成字符串
        body.put("query", query.getQueryObject().toJson());

        RequestEntity<Map<String, String>> requestEntity = RequestEntity.post(url)
                .headers(headers)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).body(body);

        ResponseEntity<CloudbaseDatabaseDeleteResult> response =
                restTemplate.exchange(requestEntity, CloudbaseDatabaseDeleteResult.class);

        return response.getBody();

    }


}
