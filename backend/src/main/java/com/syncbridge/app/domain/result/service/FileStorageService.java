package com.syncbridge.app.domain.result.service;

import com.syncbridge.app.global.error.CustomException;
import com.syncbridge.app.global.error.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** 회의록 파일(TXT/PDF/DOCX) 저장 및 검증. 실제 파싱은 FastAPI 파서가 담당한다. */
@Slf4j
@Service
public class FileStorageService {

  private static final Set<String> ALLOWED_EXTENSIONS = Set.of("txt", "pdf", "docx");

  private final Path uploadDir;

  public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
    this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
  }

  /**
   * 업로드 파일을 검증 후 저장하고, 저장된 파일 정보를 반환한다.
   *
   * @return 저장 결과 (원본 파일명 / 바이트)
   */
  public StoredFile store(MultipartFile file) {
    String originalFilename =
        StringUtils.cleanPath(
            file.getOriginalFilename() == null ? "meeting-note" : file.getOriginalFilename());
    String extension = extractExtension(originalFilename);

    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      throw new CustomException(ErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    try {
      Files.createDirectories(uploadDir);
      String storedName = UUID.randomUUID() + "." + extension;
      Path target = uploadDir.resolve(storedName);
      byte[] bytes = file.getBytes();
      Files.write(target, bytes);
      log.debug("회의록 파일 저장 완료: {}", target);
      return new StoredFile(originalFilename, target.toString(), bytes, file.getContentType());
    } catch (IOException e) {
      throw new CustomException(ErrorCode.FILE_STORAGE_ERROR);
    }
  }

  private String extractExtension(String filename) {
    int dotIndex = filename.lastIndexOf('.');
    if (dotIndex < 0 || dotIndex == filename.length() - 1) {
      throw new CustomException(ErrorCode.UNSUPPORTED_FILE_TYPE);
    }
    return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
  }

  /** 저장된 회의록 파일. */
  public record StoredFile(
      String originalFilename, String storedPath, byte[] bytes, String contentType) {}
}
