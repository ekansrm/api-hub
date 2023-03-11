package com.example.apihub.services.cloudbase;

import com.example.apihub.services.cloudbase.dto.*;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class CloudbaseDatabaseServiceTest {
    private final String secretId = "AKIDCGGXdT4ScYIp8D3g5IXcWCgvviNo1zkI";
    private final String secretKey = "OXSBgwxkIbfU8XhcmiPsBG6h3C6mGm59";
    private final String envId = "cloudbase-baas-9gq294cp867553eb";
    private final String collection = "test123";

    private TestDTO testVal1() {
        TestDTO dto = new TestDTO();
        dto.setStr("str");
        dto.setIntVal(123);
        dto.setLongVal(1312112312313123123L);

        dto.setList(Arrays.asList(1, "2", Arrays.asList("4", "5")));
        Map<String, String> mapS = new HashMap<>();
        mapS.put("k", "1");
        mapS.put("v", "2");
        dto.setMapStr(mapS);

        Map<String, Object> mapO = new HashMap<>();
        mapO.put("k", 1);
        mapO.put("v", "2");
        mapO.put("l", Arrays.asList(1, 2, 3, 4));
        dto.setMapObj(mapO);

        return dto;

    }

    @Test
    public void testDoc() {


        CloudbaseDatabaseService<TestDTO> service =
                new CloudbaseDatabaseService<>(secretId, secretKey, envId, collection, TestDTO.class);

        String docId = "abcd";

        Document insertDoc = new Document();
        insertDoc.put("1", 3);
        insertDoc.put("2", 4);
        CloudbaseDatabaseInsertResult insertOneResult = service.insertDocument(docId, insertDoc);

        Document insertDoc2 = new Document("_id", "1234");
        insertDoc2.put("3", "3");
        Document insertDoc3 = new Document();
        insertDoc3.put("w", "3");
        CloudbaseDatabaseInsertResult insertManyResult = service.insertDocuments(Arrays.asList(insertDoc2, insertDoc3));

        Document setDoc1 = new Document();
        setDoc1.put("a", 3);
        setDoc1.put("b", 4);
        CloudbaseDatabaseUpdateResult setDoc1Result = service.setDocument(docId, setDoc1);

        Document updateDoc1 = new Document();
        updateDoc1.put("a", 1);
        updateDoc1.put("c", 2);
        CloudbaseDatabaseUpdateResult updateDoc1Result = service.updateDocument(docId, updateDoc1);

        CloudbaseDatabaseFindResult getDoc1Result = service.getDocument(docId);
        CloudbaseDatabaseFindResult getNonExistsDocResult = service.getDocument("kkkkkkkk");

        Query queryOne = new Query();
        queryOne.addCriteria(Criteria.where("_id").is("1234"));
        CloudbaseDatabaseCountResult countDoc1Result = service.countDocument(queryOne);

        Query queryAll = new Query();
        CloudbaseDatabaseCountResult countAllResult = service.countDocument(queryAll);

        CloudbaseDatabaseFindResult findAllResult = service.findDocument(queryAll);

        Update update1234 = new Update();
        update1234.set("kkk", System.currentTimeMillis());
        CloudbaseDatabaseUpdateResult updateOneResult = service.updateOneDocument(queryOne, update1234);

        Query queryMany = new Query();
        queryMany.addCriteria(Criteria.where("w").exists(Boolean.TRUE));
        Update updateMany = new Update();
        updateMany.set("bbb", System.currentTimeMillis());
        CloudbaseDatabaseUpdateResult updateManyResult = service.updateManyDocument(queryMany, updateMany);

        CloudbaseDatabaseDeleteResult deleteDoc1Result = service.deleteDocument(docId);

        CloudbaseDatabaseDeleteResult deleteOneResult = service.deleteOneDocument(queryOne);

        CloudbaseDatabaseDeleteResult deleteMany = service.deleteManyDocument(queryMany);

        // 删除操作, query 不能为空
        CloudbaseDatabaseDeleteResult deleteAll = service.deleteManyDocument(queryAll);


        System.out.println();

    }


    @Test
    public void testDto() {

        CloudbaseDatabaseService<TestDTO> service =
                new CloudbaseDatabaseService<>(secretId, secretKey, envId, collection, TestDTO.class);

        TestDTO dto = testVal1();

        String id = "12345678";
        service.saveById(id, dto);
        TestDTO dto1 = service.findById(id);

        // 修改基础字段
        dto.setStr("update1234");
        service.saveById(id, dto);
        TestDTO dto2 = service.findById(id);


        // 修改列表
        dto.setList(Arrays.asList("1", "a"));
        service.saveById(id, dto);
        TestDTO dto3 = service.findById(id);

        // 修改字典
        Map<String, Object> mapO = new HashMap<>(dto.getMapObj());
        mapO.remove("v");
        mapO.put("k", 123);
        mapO.put("a", 123);
        dto.setMapObj(mapO);
        service.saveById(id, dto);
        TestDTO dto4 = service.findById(id);


        // 修改字典的列表


        // 修改列表的字典


        List<TestDTO> insertAll = service.insert(Arrays.asList(testVal1(), testVal1(), testVal1()));

        List<TestDTO> all = service.find(new Query());
        List<TestDTO> all2 = service.findAll();

        DeleteResult deleteOne = service.remove(id);

        DeleteResult deleteAll = service.removeAll();


        System.out.println();

    }
}