package com.hazem.worklink.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private Path certificatesPath;
    private Path profilePicturesPath;
    private Path cvsPath;
    private Path companyLogosPath;
    private Path portfolioImagesPath;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final long MAX_CV_SIZE = 10 * 1024 * 1024; // 10MB for CVs
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "jpg", "jpeg", "png");
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private static final List<String> ALLOWED_CV_EXTENSIONS = Arrays.asList("pdf", "doc", "docx");

    @PostConstruct
    public void init() {
        try {
            certificatesPath = Paths.get(uploadDir, "certificates").toAbsolutePath().normalize();
            profilePicturesPath = Paths.get(uploadDir, "profile-pictures").toAbsolutePath().normalize();
            cvsPath = Paths.get(uploadDir, "cvs").toAbsolutePath().normalize();
            companyLogosPath = Paths.get(uploadDir, "company-logos").toAbsolutePath().normalize();
            portfolioImagesPath = Paths.get(uploadDir, "portfolio-images").toAbsolutePath().normalize();
            Files.createDirectories(certificatesPath);
            Files.createDirectories(profilePicturesPath);
            Files.createDirectories(cvsPath);
            Files.createDirectories(companyLogosPath);
            Files.createDirectories(portfolioImagesPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directories", e);
        }
    }

    public String storeCertificate(MultipartFile file) {
        validateFile(file);

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String newFileName = UUID.randomUUID().toString() + "." + fileExtension;

        try {
            Path targetLocation = certificatesPath.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return "/api/files/certificates/" + newFileName;
        } catch (IOException e) {
            throw new RuntimeException("Could not store file " + originalFileName, e);
        }
    }

    public Path getCertificatePath(String fileName) {
        return certificatesPath.resolve(fileName).normalize();
    }

    public String storeProfilePicture(MultipartFile file) {
        validateImageFile(file);

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String newFileName = UUID.randomUUID().toString() + "." + fileExtension;

        try {
            Path targetLocation = profilePicturesPath.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return "/api/files/profile-pictures/" + newFileName;
        } catch (IOException e) {
            throw new RuntimeException("Could not store profile picture " + originalFileName, e);
        }
    }

    public Path getProfilePicturePath(String fileName) {
        return profilePicturesPath.resolve(fileName).normalize();
    }

    public String storeCv(MultipartFile file) {
        validateCvFile(file);

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String newFileName = UUID.randomUUID().toString() + "." + fileExtension;

        try {
            Path targetLocation = cvsPath.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return "/api/files/cvs/" + newFileName;
        } catch (IOException e) {
            throw new RuntimeException("Could not store CV " + originalFileName, e);
        }
    }

    public Path getCvPath(String fileName) {
        return cvsPath.resolve(fileName).normalize();
    }

    public String storeCompanyLogo(MultipartFile file) {
        validateImageFile(file);

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String newFileName = UUID.randomUUID().toString() + "." + fileExtension;

        try {
            Path targetLocation = companyLogosPath.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return "/api/files/company-logos/" + newFileName;
        } catch (IOException e) {
            throw new RuntimeException("Could not store company logo " + originalFileName, e);
        }
    }

    public Path getCompanyLogoPath(String fileName) {
        return companyLogosPath.resolve(fileName).normalize();
    }

    public String storePortfolioImage(MultipartFile file) {
        validateImageFile(file);

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String newFileName = UUID.randomUUID().toString() + "." + fileExtension;

        try {
            Path targetLocation = portfolioImagesPath.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return "/api/files/portfolio-images/" + newFileName;
        } catch (IOException e) {
            throw new RuntimeException("Could not store portfolio image " + originalFileName, e);
        }
    }

    public Path getPortfolioImagePath(String fileName) {
        return portfolioImagesPath.resolve(fileName).normalize();
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 5MB");
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getFileExtension(originalFileName).toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("File type not allowed. Allowed types: " + ALLOWED_EXTENSIONS);
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 5MB");
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getFileExtension(originalFileName).toLowerCase();

        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("File type not allowed. Allowed types: " + ALLOWED_IMAGE_EXTENSIONS);
        }
    }

    private void validateCvFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_CV_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 10MB");
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getFileExtension(originalFileName).toLowerCase();

        if (!ALLOWED_CV_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("File type not allowed. Allowed types: " + ALLOWED_CV_EXTENSIONS);
        }
    }
}
