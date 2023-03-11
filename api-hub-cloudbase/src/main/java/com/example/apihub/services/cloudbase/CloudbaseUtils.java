package com.example.apihub.services.cloudbase;

import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;

public class CloudbaseUtils {

    private static final Charset UTF8 = StandardCharsets.UTF_8;

    private static byte[] hmac256(byte[] key, String msg) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, mac.getAlgorithm());
        mac.init(secretKeySpec);
        return mac.doFinal(msg.getBytes(UTF8));
    }

    private static String sha256Hex(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] d = md.digest(s.getBytes(UTF8));
        return DatatypeConverter.printHexBinary(d).toLowerCase();
    }

    @SneakyThrows
    public static HttpHeaders buildHeaders(String secretId, String secretKey) {

        String service = "tcb";//固定值
        String version = "1.0";
        String algorithm = "TC3-HMAC-SHA256";
        String token = "";
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        // 注意时区，否则容易出错
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String date = sdf.format(new Date(Long.parseLong(timestamp + "000")));

        // ************* 步骤 1：拼接规范请求串(此处无需操作，都是固定值) *************
        String signedHeaders = "content-type;host";
        String canonicalRequest = "POST\n//api.tcloudbase.com/\n\ncontent-type:application/json; charset=utf-8\nhost:api.tcloudbase.com\n\ncontent-type;host\ne3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        // ************* 步骤 2：拼接待签名字符串(和示例代码基本无出入) *************
        String credentialScope = date + "/" + service + "/" + "tc3_request";
        String hashedCanonicalRequest = sha256Hex(canonicalRequest);
        String stringToSign = algorithm + "\n" + timestamp + "\n" + credentialScope + "\n" + hashedCanonicalRequest;
        System.out.println(stringToSign);

        // ************* 步骤 3：计算签名(和示例代码基本无出入) *************
        byte[] secretDate = hmac256(("TC3" + secretKey).getBytes(UTF8), date);
        byte[] secretService = hmac256(secretDate, service);
        byte[] secretSigning = hmac256(secretService, "tc3_request");
        String signature = DatatypeConverter.printHexBinary(hmac256(secretSigning, stringToSign)).toLowerCase();
        System.out.println(signature);

        // ************* 步骤 4：拼接 Authorization(和示例代码基本无出入) *************
        String authorization = algorithm + " " + "Credential=" + secretId + "/" + credentialScope + ", "
                + "SignedHeaders=" + signedHeaders + ", " + "Signature=" + signature;
        System.out.println(authorization);

        // ************* 步骤 5：拼接 请求头(补充) *************
        HttpHeaders headers = new HttpHeaders();
        headers.put("X-CloudBase-Authorization", Collections.singletonList(version + " " + authorization));
        headers.put("X-CloudBase-SessionToken", Collections.singletonList(token));
        headers.put("X-CloudBase-TimeStamp", Collections.singletonList(timestamp));

        return headers;
    }

}
