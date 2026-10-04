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
import java.nio.charset.StandardCharsets;
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

    private static byte[] audioHeader(int length, int offset, String ascii) {
        byte[] data = new byte[length];
        byte[] chars = ascii.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(chars, 0, data, offset, chars.length);
        return data;
    }

    // ------------------------------------------------------------------
    // detectAudioExtension() Tests
    // ------------------------------------------------------------------

    @Test
    void detectAudioExtension_id3Header_shouldReturnMp3() {
        assertEquals("mp3", FileStorageServiceImpl.detectAudioExtension(audioHeader(12, 0, "ID3")));
    }

    @Test
    void detectAudioExtension_headerShorterThanNeeded_shouldReturnNullWithoutThrowing() {
        assertNull(FileStorageServiceImpl.detectAudioExtension(new byte[] { 'R', 'I' }));
    }

    @Test
    void detectAudioExtension_mp3FrameSync_shouldReturnMp3() {
        byte[] header = new byte[12];
        header[0] = (byte) 0xFF;
        header[1] = (byte) 0xFB; // Frame sync chuẩn của MP3
        assertEquals("mp3", FileStorageServiceImpl.detectAudioExtension(header));
    }

    @Test
    void detectAudioExtension_wav_shouldReturnWav() {
        byte[] header = new byte[12];
        byte[] riff = "RIFF".getBytes(StandardCharsets.US_ASCII);
        byte[] wave = "WAVE".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(riff, 0, header, 0, 4);
        System.arraycopy(wave, 0, header, 8, 4);
        assertEquals("wav", FileStorageServiceImpl.detectAudioExtension(header));
    }

    @Test
    void detectAudioExtension_ogg_shouldReturnOgg() {
        assertEquals("ogg", FileStorageServiceImpl.detectAudioExtension(audioHeader(12, 0, "OggS")));
    }

    @Test
    void detectAudioExtension_m4a_shouldReturnM4a() {
        assertEquals("m4a", FileStorageServiceImpl.detectAudioExtension(audioHeader(12, 4, "ftyp")));
    }

    @Test
    void detectAudioExtension_jpegHeader_shouldReturnNull() {
        byte[] jpegHeader = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0 };
        assertNull(FileStorageServiceImpl.detectAudioExtension(jpegHeader));
    }

    @Test
    void detectAudioExtension_plainText_shouldReturnNull() {
        byte[] textHeader = "Hello world!".getBytes(StandardCharsets.US_ASCII);
        assertNull(FileStorageServiceImpl.detectAudioExtension(textHeader));
    }

    // ------------------------------------------------------------------
    // storeAudio() Tests
    // ------------------------------------------------------------------

    @Test
    void storeAudio_happyPathMp3_shouldReturnUrlUnderDictationAndWriteFile() throws IOException {
        MockMultipartFile mp3 = new MockMultipartFile("file", "lesson.mp3", "audio/mpeg", audioHeader(64, 0, "ID3"));
        String url = fileStorageService.storeAudio(mp3, "dictation");
        assertTrue(url.startsWith("/uploads/dictation/"));
        assertTrue(url.endsWith(".mp3"));
        assertTrue(Files.exists(tempDir.resolve("dictation").resolve(url.substring("/uploads/dictation/".length()))));
    }

    @Test
    void storeAudio_whenFileIsNullOrEmpty_shouldThrowInvalidFileException() {
        assertThrows(InvalidFileException.class, () -> fileStorageService.storeAudio(null, "dictation"));

        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.mp3", "audio/mpeg", new byte[0]);
        assertThrows(InvalidFileException.class, () -> fileStorageService.storeAudio(emptyFile, "dictation"));
    }

    @Test
    void storeAudio_whenFileExceedsMaxSize_shouldThrowInvalidFileException() {
        FileStorageServiceImpl customStorage = new FileStorageServiceImpl(
                tempDir.toString(), MAX_FILE_SIZE_MB, ALLOWED_CONTENT_TYPES, 1L);

        byte[] content = new byte[1024 * 1024 + 1]; // 1MB + 1 byte
        byte[] header = audioHeader(12, 0, "ID3");
        System.arraycopy(header, 0, content, 0, header.length);

        MockMultipartFile oversizedFile = new MockMultipartFile("file", "big.mp3", "audio/mpeg", content);

        assertThrows(InvalidFileException.class, () -> customStorage.storeAudio(oversizedFile, "dictation"));
    }

    @Test
    void storeAudio_whenContentIsNotAudio_shouldThrowInvalidFileException() {
        MockMultipartFile textFile = new MockMultipartFile("file", "lesson.mp3", "audio/mpeg",
                "plain text content".getBytes());
        assertThrows(InvalidFileException.class, () -> fileStorageService.storeAudio(textFile, "dictation"));
    }

    @Test
    void storeAudio_whenRealImageUploadedAsAudio_shouldThrowInvalidFileException() throws IOException {
        byte[] pngBytes = generateRealImageBytes("png");
        MockMultipartFile fakeAudio = new MockMultipartFile("file", "song.mp3", "audio/mpeg", pngBytes);
        assertThrows(InvalidFileException.class, () -> fileStorageService.storeAudio(fakeAudio, "dictation"));
    }

    @Test
    void storeAudio_extensionComesFromContentNotFilename() {
        MockMultipartFile fileWithWrongExt = new MockMultipartFile("file", "evil.exe", "application/octet-stream",
                audioHeader(64, 0, "ID3"));
        String url = fileStorageService.storeAudio(fileWithWrongExt, "dictation");
        assertTrue(url.endsWith(".mp3"));
    }

    @Test
    void storeAudio_whenTargetFolderIsBlockedByExistingFile_shouldThrowRuntimeException() throws IOException {
        Path blocker = tempDir.resolve("dictation");
        Files.createFile(blocker);

        MockMultipartFile mp3 = new MockMultipartFile("file", "lesson.mp3", "audio/mpeg", audioHeader(64, 0, "ID3"));
        assertThrows(RuntimeException.class, () -> fileStorageService.storeAudio(mp3, "dictation"));
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
        assertTrue(exception.getMessage().contains("not a valid image"));
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

    @Test
    void detectAudioExtension_null_shouldReturnNull() {
        assertNull(FileStorageServiceImpl.detectAudioExtension(null));
    }

    @Test
    void detectAudioExtension_riffButNotWave_shouldReturnNull() {
        byte[] header = audioHeader(12, 0, "RIFF");
        System.arraycopy("AVI ".getBytes(StandardCharsets.US_ASCII), 0, header, 8, 4);
        assertNull(FileStorageServiceImpl.detectAudioExtension(header));
    }
}