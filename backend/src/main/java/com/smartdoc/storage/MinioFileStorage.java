package com.smartdoc.storage;

import io.minio.*;
import java.io.InputStream;
import java.util.UUID;

public class MinioFileStorage implements FileStorage {
    private final MinioClient client; private final String bucket;
    public MinioFileStorage(MinioClient client,String bucket){this.client=client;this.bucket=bucket;}
    public String save(String originalName,InputStream input) throws Exception{
        ensureBucket(); String key="documents/"+UUID.randomUUID()+".pdf";
        client.putObject(PutObjectArgs.builder().bucket(bucket).object(key).stream(input,-1,10*1024*1024).contentType("application/pdf").build());return key;
    }
    public InputStream open(String key) throws Exception{return client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build());}
    public void delete(String key) throws Exception{client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());}
    private void ensureBucket() throws Exception{if(!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()))client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());}
}
