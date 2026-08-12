package com.smartdoc.storage;
import java.io.InputStream;
public interface FileStorage {
    String save(String originalName, InputStream input) throws Exception;
    InputStream open(String key) throws Exception;
    void delete(String key) throws Exception;
}
