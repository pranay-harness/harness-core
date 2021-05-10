package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.GitSyncSettings;
import io.harness.gitsync.common.dtos.GitSyncSettingsDTO;
import io.harness.gitsync.common.remote.GitSyncSettingsMapper;
import io.harness.gitsync.common.service.GitSyncSettingsService;
import io.harness.repositories.gitSyncSettings.GitSyncSettingsRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import javax.ws.rs.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(DX)
public class GitSyncSettingsServiceImpl implements GitSyncSettingsService {
  private final GitSyncSettingsRepository gitSyncSettingsRepository;

  @Override
  public GitSyncSettingsDTO get(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    final Optional<GitSyncSettings> gitSyncSettings =
        gitSyncSettingsRepository.findByAccountIdentifierAndProjectIdentifierAndOrgIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier);
    return gitSyncSettings.map(GitSyncSettingsMapper::getDTOFromGitSyncSettings)
        .orElseThrow(
            ()
                -> new NotFoundException(String.format(
                    "No Git Sync Setting found for accountIdentifier %s, organizationIdentifier %s and projectIdentifier %s",
                    accountIdentifier, orgIdentifier, projectIdentifier)));
  }

  @Override
  public GitSyncSettingsDTO save(GitSyncSettingsDTO request) {
    GitSyncSettings gitSyncSettings = GitSyncSettingsMapper.getGitSyncSettingsFromDTO(request);
    final GitSyncSettings savedGitSyncSettings = gitSyncSettingsRepository.save(gitSyncSettings);
    return GitSyncSettingsMapper.getDTOFromGitSyncSettings(savedGitSyncSettings);
  }
}
