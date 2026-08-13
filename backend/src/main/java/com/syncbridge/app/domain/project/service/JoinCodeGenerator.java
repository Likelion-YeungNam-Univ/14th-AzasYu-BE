package com.syncbridge.app.domain.project.service;

import com.syncbridge.app.domain.project.repository.ProjectRepository;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 프로젝트 참여 코드 생성기. 형식: {@code A7K9-M2P4} (혼동하기 쉬운 문자 0/O/1/I 제외). */
@Component
@RequiredArgsConstructor
public class JoinCodeGenerator {

  private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private static final int BLOCK_SIZE = 4;
  private static final int MAX_ATTEMPTS = 10;

  private final SecureRandom random = new SecureRandom();
  private final ProjectRepository projectRepository;

  public String generate() {
    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
      String code = randomBlock() + "-" + randomBlock();
      if (!projectRepository.existsByJoinCode(code)) {
        return code;
      }
    }
    throw new IllegalStateException("참여 코드 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");
  }

  private String randomBlock() {
    StringBuilder block = new StringBuilder(BLOCK_SIZE);
    for (int i = 0; i < BLOCK_SIZE; i++) {
      block.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
    }
    return block.toString();
  }
}
