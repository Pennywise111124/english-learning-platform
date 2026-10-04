package com.example.englishlearningplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
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

    private static final long DEFAULT_MAX_AUDIO_SIZE_MB = 10L;
    private static final int AUDIO_HEADER_LENGTH = 12;

    private final String uploadDir;
    private final long maxFileSizeBytes;
    private final List<String> allowedContentTypes;
    private final long maxAudioSizeBytes;

    public FileStorageServiceImpl(String uploadDir, long maxFileSizeMb, String[] allowedContentTypes) {
        this(uploadDir, maxFileSizeMb, allowedContentTypes, DEFAULT_MAX_AUDIO_SIZE_MB);
    }

    @Autowired
    public FileStorageServiceImpl(
            @Value("${app.upload.dir}") String uploadDir,
            @Value("${app.upload.max-file-size-mb}") long maxFileSizeMb,
            @Value("${app.upload.allowed-content-types}") String[] allowedContentTypes,
            @Value("${app.upload.audio-max-file-size-mb}") long maxAudioSizeMb) {
        this.uploadDir = uploadDir;
        this.maxFileSizeBytes = maxFileSizeMb * 1024 * 1024;
        this.allowedContentTypes = Arrays.asList(allowedContentTypes);
        this.maxAudioSizeBytes = maxAudioSizeMb * 1024 * 1024;
    }

    static String detectAudioExtension(byte[] header) {
        if (header == null) {
            return null;
        }

        boolean isMp3Id3 = startsWithAscii(header, 0, "ID3");
        boolean isMp3Sync = header.length >= 2
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xE0) == 0xE0;

        if (isMp3Id3 || isMp3Sync) {
            return "mp3";
        }

        if (startsWithAscii(header, 0, "RIFF") && startsWithAscii(header, 8, "WAVE")) {
            return "wav";
        }

        if (startsWithAscii(header, 0, "OggS")) {
            return "ogg";
        }

        if (startsWithAscii(header, 4, "ftyp")) {
            return "m4a";
        }

        return null;
    }

    private static boolean startsWithAscii(byte[] data, int offset, String ascii) {
        if (data == null || offset < 0 || data.length < offset + ascii.length()) {
            return false;
        }
        for (int i = 0; i < ascii.length(); i++) {
            if (data[offset + i] != (byte) ascii.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String storeAudio(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is missing or empty");
        }

        if (file.getSize() > maxAudioSizeBytes) {
            throw new InvalidFileException("File size exceeds the limit");
        }

        byte[] header;
        try (InputStream in = file.getInputStream()) {
            header = in.readNBytes(AUDIO_HEADER_LENGTH);
        } catch (IOException e) {
            throw new InvalidFileException("Error while reading the audio file");
        }

        String ext = detectAudioExtension(header);
        if (ext == null) {
            throw new InvalidFileException("Unsupported audio format (only MP3/WAV/OGG/M4A are accepted)");
        }

        return saveToDisk(file, subDirectory, ext);
    }

    @Override
    public String storeImage(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is missing or empty");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new InvalidFileException("File size exceeds the limit");
        }

        String contentType = file.getContentType();

        boolean isValidContentType = contentType != null &&
                (allowedContentTypes.contains(contentType) || contentType.equals("application/octet-stream"));

        if (!isValidContentType) {
            throw new InvalidFileException("Unsupported file format (only JPG/PNG are accepted)");
        }

        String extension;
        try (InputStream inputStream = file.getInputStream();
                ImageInputStream iis = ImageIO.createImageInputStream(inputStream)) {

            if (iis == null) {
                throw new InvalidFileException("Unable to read the file stream");
            }

            var readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new InvalidFileException("File content is not a valid image!");
            }

            ImageReader reader = readers.next();
            reader.setInput(iis);

            BufferedImage bufferedImage = reader.read(0);
            if (bufferedImage == null) {
                throw new InvalidFileException("Image file is corrupted or unreadable!");
            }
            String formatName = reader.getFormatName().toLowerCase();

            if (!formatName.equals("jpeg") && !formatName.equals("jpg") && !formatName.equals("png")) {
                throw new InvalidFileException("Unsupported file format (only JPG/PNG are accepted)");
            }
            extension = formatName.equals("jpeg") ? "jpg" : formatName;

            reader.dispose();

        } catch (IOException e) {
            throw new InvalidFileException("Error while reading and validating the image file");
        }

        return saveToDisk(file, subDirectory, extension);
    }

    private String saveToDisk(MultipartFile file, String subDirectory, String extension) {
        String filename = UUID.randomUUID().toString() + "." + extension;

        Path targetFolder = Paths.get(uploadDir, subDirectory);
        try {
            Files.createDirectories(targetFolder);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create storage directory: " + targetFolder.toString(), e);
        }

        Path targetPath = targetFolder.resolve(filename);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save the file to disk", e);
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