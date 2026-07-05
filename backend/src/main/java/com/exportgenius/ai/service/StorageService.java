package com.exportgenius.ai.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {
    String store(MultipartFile file) throws IOException;
    String storeBytes(byte[] bytes, String filename) throws IOException;
    void delete(String fileUrl) throws IOException;
}
