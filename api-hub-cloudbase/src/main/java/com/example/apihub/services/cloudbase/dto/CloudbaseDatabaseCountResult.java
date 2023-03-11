package com.example.apihub.services.cloudbase.dto;

import lombok.Data;

@Data
public class CloudbaseDatabaseCountResult {

    private String code;
    private String message;
    private String requestId;
    private DataDTO data;

    @Data
    static public class DataDTO {

        private Long total;

    }

    public long getCount() {

        if(data!=null && data.total!=null) {
            return data.total;
        }
        return 0;
    }
}
