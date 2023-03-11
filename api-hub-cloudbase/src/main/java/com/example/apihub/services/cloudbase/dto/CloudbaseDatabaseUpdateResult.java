package com.example.apihub.services.cloudbase.dto;

import com.mongodb.client.result.UpdateResult;
import lombok.Data;
import org.bson.BsonArray;
import org.bson.BsonString;
import org.bson.BsonValue;

@Data
public class CloudbaseDatabaseUpdateResult extends UpdateResult {

    private String code;
    private String message;
    private String requestId;
    private DataDTO data;

    @Override
    public boolean wasAcknowledged() {
        return false;
    }

    @Override
    public long getMatchedCount() {
        if(data!=null && data.matched!=null) {
            return data.matched;
        }
        return 0;
    }

    @Override
    public long getModifiedCount() {
        if(data!=null && data.updated!=null) {
            return data.updated;
        }

        return 0;
    }

    @Override
    public BsonValue getUpsertedId() {
        if(data!=null && data.upsert_id!=null) {
            return new BsonString(data.upsert_id);
        }
        return null;
    }

    @Data
    static public class DataDTO {
        private Integer updated;
        private Integer matched;
        private String upsert_id;

    }
}
