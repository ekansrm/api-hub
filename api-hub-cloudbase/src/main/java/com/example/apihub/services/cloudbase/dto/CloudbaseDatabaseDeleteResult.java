package com.example.apihub.services.cloudbase.dto;

import com.mongodb.client.result.DeleteResult;
import lombok.Data;

@Data
public class CloudbaseDatabaseDeleteResult extends DeleteResult {


    private String code;
    private String message;
    private String requestId;
    private DataDTO data;

    @Override
    public boolean wasAcknowledged() {
        return false;
    }

    @Override
    public long getDeletedCount() {
        if(data!=null && data.deleted!=null) {
            return data.deleted;
        }
        return 0;
    }

    @Data
    static public class DataDTO {
        private Integer deleted;

    }

}
