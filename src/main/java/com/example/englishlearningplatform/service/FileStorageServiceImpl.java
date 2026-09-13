package com.example.englishlearningplatform.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.englishlearningplatform.exception.InvalidFileException;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final String uploadDir;
    private final long maxFileSizeBytes;
    private final List<String> allowedContentTypes;

    public FileStorageServiceImpl(
            @Value("${app.upload.dir}") String uploadDir,
            @Value("${app.upload.max-file-size-mb}") long maxFileSizeMb,
            @Value("${app.upload.allowed-content-types}") String[] allowedContentTypes) {
        this.uploadDir = uploadDir;
        this.maxFileSizeBytes = maxFileSizeMb * 1024 * 1024;
        this.allowedContentTypes = Arrays.asList(allowedContentTypes);
    }

    @Override
    public String storeImage(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File không tồn tại hoặc rỗng");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new InvalidFileException("Dung lượng file vượt quá giới hạn");
        }

        String contentType = file.getContentType();

        boolean isValidContentType = contentType != null &&
                (allowedContentTypes.contains(contentType) || contentType.equals("application/octet-stream"));

        if (!isValidContentType) {
            throw new InvalidFileException("Định dạng File không được hỗ trợ (Chỉ chấp nhận JPG/PNG)");
        }

        String extension;
        try (InputStream inputStream = file.getInputStream();
                ImageInputStream iis = ImageIO.createImageInputStream(inputStream)) {

            if (iis == null) {
                throw new InvalidFileException("Không thể đọc luồng dữ liệu của tệp tin");
            }

            var readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new InvalidFileException("Nội dung tệp không phải là hình ảnh hợp lệ!");
            }

            ImageReader reader = readers.next();
            reader.setInput(iis);

            BufferedImage bufferedImage = reader.read(0);
            if (bufferedImage == null) {
                throw new InvalidFileException("Nội dung tệp hình ảnh bị lỗi hoặc hỏng!");
            }
            String formatName = reader.getFormatName().toLowerCase();

            if (!formatName.equals("jpeg") && !formatName.equals("jpg") && !formatName.equals("png")) {
                throw new InvalidFileException("Định dạng File không được hỗ trợ (Chỉ chấp nhận JPG/PNG)");
            }
            extension = formatName.equals("jpeg") ? "jpg" : formatName;

            reader.dispose();

        } catch (IOException e) {
            throw new InvalidFileException("Lỗi trong quá trình đọc và kiểm tra tệp hình ảnh");
        }

        String filename = UUID.randomUUID().toString() + "." + extension;

        Path targetFolder = Paths.get(uploadDir, subDirectory);
        try {
            Files.createDirectories(targetFolder);
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo thư mục lưu trữ file: " + targetFolder.toString(), e);
        }

        Path targetPath = targetFolder.resolve(filename);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi lưu tệp tin vào đĩa cứng", e);
        }

        return "/uploads/" + subDirectory + "/" + filename;
    }

    @Override
    public void deleteFile(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.isBlank() || relativeUrl.contains("default")) {
            return;
        }
        try {
            String pathInDisk = relativeUrl.replaceFirst("^/uploads/", "");
            Path targetPath = Paths.get(uploadDir, pathInDisk);
            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            System.err.println("Không thể xóa file cũ: " + relativeUrl + " - " + e.getMessage());
        }
    }
}