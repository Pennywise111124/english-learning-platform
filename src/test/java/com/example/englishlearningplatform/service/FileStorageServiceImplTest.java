package com.example.englishlearningplatform.service;

import com.example.englishlearningplatform.exception.InvalidFileException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceImplTest {

    @TempDir
    Path tempDir;

    private FileStorageServiceImpl fileStorageService;

    private static final long MAX_FILE_SIZE_MB = 1L;
    private static final String[] ALLOWED_CONTENT_TYPES = { "image/jpeg", "image/png" };

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageServiceImpl(
                tempDir.toString(),
                MAX_FILE_SIZE_MB,
                ALLOWED_CONTENT_TYPES);
    }

    private byte[] generateRealImageBytes(String formatName) throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean written = ImageIO.write(image, formatName, baos);
        assertTrue(written, "Không tìm được ImageWriter cho format: " + formatName
                + " — JDK hiện tại có thể thiếu plugin này");
        return baos.toByteArray();
    }

    // ------------------------------------------------------------------
    // storeImage()
    // ------------------------------------------------------------------

    @Test
    void storeImage_whenFileIsNull_shouldThrowInvalidFileException() {
        assertThrows(InvalidFileException.class, () -> fileStorageService.storeImage(null, "topics"));
    }

    @Test
    void storeImage_whenFileIsEmpty_shouldThrowInvalidFileException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThrows(InvalidFileException.class, () -> fileStorageService.storeImage(emptyFile, "topics"));
    }

    @Test
    void storeImage_whenFileExceedsMaxSize_shouldThrowInvalidFileException() throws IOException {
        byte[] oversizedContent = new byte[(int) (MAX_FILE_SIZE_MB * 1024 * 1024) + 1];
        MockMultipartFile oversizedFile = new MockMultipartFile(
                "file", "big.jpg", "image/jpeg", oversizedContent);

        assertThrows(InvalidFileException.class, () -> fileStorageService.storeImage(oversizedFile, "topics"));
    }

    @Test
    void storeImage_whenContentTypeNotAllowed_shouldThrowInvalidFileException() {
        MockMultipartFile textFile = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "hello world".getBytes());

        assertThrows(InvalidFileException.class, () -> fileStorageService.storeImage(textFile, "topics"));
    }

    @Test
    void storeImage_whenContentIsNotARealImage_shouldThrowInvalidFileException() {
        MockMultipartFile fakeImage = new MockMultipartFile(
                "file", "fake.jpg", "image/jpeg", "not a real image".getBytes());

        InvalidFileException exception = assertThrows(InvalidFileException.class,
                () -> fileStorageService.storeImage(fakeImage, "topics"));
        assertTrue(exception.getMessage().contains("không phải là hình ảnh hợp lệ"));
    }

    @Test
    void storeImage_whenImageFormatNotSupported_shouldThrowInvalidFileException() throws IOException {
        byte[] bmpBytes = generateRealImageBytes("bmp");
        MockMultipartFile bmpFile = new MockMultipartFile(
                "file", "image.bmp", "application/octet-stream", bmpBytes);

        assertThrows(InvalidFileException.class, () -> fileStorageService.storeImage(bmpFile, "topics"));
    }

    @Test
    void storeImage_happyPathJpeg_shouldReturnUrlWithJpgExtensionAndWriteFileToDisk() throws IOException {
        byte[] jpegBytes = generateRealImageBytes("jpeg");
        MockMultipartFile jpegFile = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", jpegBytes);

        String url = fileStorageService.storeImage(jpegFile, "topics");

        assertNotNull(url);
        assertTrue(url.startsWith("/uploads/topics/"));
        assertTrue(url.endsWith(".jpg"));

        String filename = url.substring("/uploads/topics/".length());
        Path savedFile = tempDir.resolve("topics").resolve(filename);

        assertTrue(Files.exists(savedFile), "File thật sự phải tồn tại trên ổ đĩa");
    }

    @Test
    void storeImage_happyPathPng_shouldKeepPngExtension() throws IOException {
        byte[] pngBytes = generateRealImageBytes("png");
        MockMultipartFile pngFile = new MockMultipartFile(
                "file", "photo.png", "image/png", pngBytes);

        String url = fileStorageService.storeImage(pngFile, "topics");

        assertNotNull(url);
        assertTrue(url.startsWith("/uploads/topics/"));
        assertTrue(url.endsWith(".png"));

        String filename = url.substring("/uploads/topics/".length());
        Path savedFile = tempDir.resolve("topics").resolve(filename);

        assertTrue(Files.exists(savedFile), "File PNG thật sự phải tồn tại trên ổ đĩa");
    }

    @Test
    void storeImage_whenTargetFolderIsBlockedByExistingFile_shouldThrowRuntimeException() throws IOException {
        Path blockerFile = tempDir.resolve("topics");
        Files.createFile(blockerFile);

        byte[] jpegBytes = generateRealImageBytes("jpeg");
        MockMultipartFile jpegFile = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", jpegBytes);

        assertThrows(RuntimeException.class, () -> fileStorageService.storeImage(jpegFile, "topics"));
    }

    // ------------------------------------------------------------------
    // deleteFile()
    // ------------------------------------------------------------------

    @Test
    void deleteFile_whenUrlIsNull_shouldDoNothingWithoutThrowing() {
        assertDoesNotThrow(() -> fileStorageService.deleteFile(null));
    }

    @Test
    void deleteFile_whenUrlIsBlank_shouldDoNothingWithoutThrowing() {
        assertDoesNotThrow(() -> fileStorageService.deleteFile("   "));
    }

    @Test
    void deleteFile_whenUrlContainsDefault_shouldNotDeleteFile() throws IOException {
        Path defaultAvatarDir = tempDir.resolve("avatars");
        Files.createDirectories(defaultAvatarDir);
        Path defaultAvatarFile = defaultAvatarDir.resolve("default-avatar.png");
        Files.writeString(defaultAvatarFile, "fake content");

        fileStorageService.deleteFile("/uploads/avatars/default-avatar.png");

        assertTrue(Files.exists(defaultAvatarFile), "File có chữ 'default' không được phép bị xoá");
    }

    @Test
    void deleteFile_happyPath_shouldDeleteExistingFileFromDisk() throws IOException {
        Path subDir = tempDir.resolve("topics");
        Files.createDirectories(subDir);
        Path realFile = subDir.resolve("photo.jpg");
        Files.writeString(realFile, "fake content");

        fileStorageService.deleteFile("/uploads/topics/photo.jpg");

        assertFalse(Files.exists(realFile), "File thật sự phải bị xoá khỏi đĩa");
    }

    @Test
    void deleteFile_whenFileDoesNotExistOnDisk_shouldNotThrow() {
        assertDoesNotThrow(() -> fileStorageService.deleteFile("/uploads/topics/non_existent.jpg"));
    }
}