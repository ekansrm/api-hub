package com.example.apihub.services.cloudbase;

import lombok.Data;

@Data
public class CloudbaseResponse<T> {

    private String code;

    private String message;

    private String requestId;

    private T data;


}
