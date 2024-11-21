package com.carpetadigital.ecommerce.Firebase;

import com.google.cloud.storage.BlobId;
import lombok.Data;

@Data
public class ResFirebase {

    private BlobId fileId;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private String publicaLink;
    private String privadaLink;
    private Long createTime;
    private String downLoadToken;
    private String downLoadUrl;

}
