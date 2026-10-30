package com.doch.Catalog.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.ServletContext;

@Service
public class FileStorageService {

    @Autowired
    ServletContext context;
    private Path foundFile;

    public String saveFile(String fileName, MultipartFile multipartFile, Path uploadPath) throws Exception {
          
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
            } catch (IOException e) {
                throw new Exception("Could not create the directory where the uploaded files will be stored.");
            }
        }
 
        String fileCode = RandomStringUtils.secure().next(8);
         
        try (InputStream inputStream = multipartFile.getInputStream()) {
            if (fileName.contains("..")) {
                throw new Exception("Sorry! Filename contains invalid path sequence " + fileName);
            }
            Path filePath = uploadPath.resolve(fileCode + "-" + fileName);
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ioe) {       
            throw new IOException("Could not save file: " + fileName, ioe);
        }
         
        return fileCode;
    }

    public File getFilePath(String modifiedFileName, String path) throws IOException {
        boolean exist = new File(context.getRealPath("/"+path+"/")).exists();
        if (!exist) {
            System.out.println("Creating the directory");
            new File(context.getRealPath("/"+path+"/")).mkdir();
            Path uploadPath = Paths.get("/"+path+"/");
            Files.createDirectories(uploadPath);
            System.out.println("Getting file path " + uploadPath);
        }
        String modifiedFilePath = context.getRealPath("/"+path+"/"+File.separator+modifiedFileName);
        File file = new File(modifiedFilePath);
        return file;
    }

    public Resource getFileAsByte(String fileCode) throws IOException {
        Path dirPath = Paths.get("Files-Upload");
         
        Files.list(dirPath).forEach(file -> {
            if (file.getFileName().toString().startsWith(fileCode)) {
                foundFile = file;
                return;
            }
        });
 
        if (foundFile == null) {
            // File not found
            return null;
        }

        // return Files.readAllBytes(foundFile);
        return new UrlResource(foundFile.toUri());
    }
}
