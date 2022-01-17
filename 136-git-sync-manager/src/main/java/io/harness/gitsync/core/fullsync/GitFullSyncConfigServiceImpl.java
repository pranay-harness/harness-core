/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.core.fullsync;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.gitsync.core.beans.GitFullSyncConfig;
import io.harness.gitsync.fullsync.dtos.GitFullSyncConfigDTO;
import io.harness.gitsync.fullsync.dtos.GitFullSyncConfigRequestDTO;
import io.harness.gitsync.fullsync.remote.mappers.GitFullSyncConfigMapper;
import io.harness.repositories.fullSync.GitFullSyncConfigRepository;

import com.google.inject.Inject;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.PL)
@Slf4j
public class GitFullSyncConfigServiceImpl implements GitFullSyncConfigService {
  private final GitFullSyncConfigRepository gitFullSyncConfigRepository;

  @Override
  public GitFullSyncConfigDTO createConfig(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, GitFullSyncConfigRequestDTO dto) {
    if (get(accountIdentifier, orgIdentifier, projectIdentifier).isPresent()) {
      throw new InvalidRequestException(
          "Configuration already exists, please update it instead of creating a new one", WingsException.USER);
    }
    GitFullSyncConfig gitFullSyncConfig =
        GitFullSyncConfigMapper.fromDTO(accountIdentifier, orgIdentifier, projectIdentifier, dto);
    return GitFullSyncConfigMapper.toDTO(gitFullSyncConfigRepository.save(gitFullSyncConfig));
  }

  @Override
  public Optional<GitFullSyncConfigDTO> get(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return getInternal(accountIdentifier, orgIdentifier, projectIdentifier).map(GitFullSyncConfigMapper::toDTO);
  }

  private Optional<GitFullSyncConfig> getInternal(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return gitFullSyncConfigRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier);
  }

  @Override
  public GitFullSyncConfigDTO updateConfig(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, GitFullSyncConfigRequestDTO dto) {
    Optional<GitFullSyncConfig> gitFullSyncConfigOptional =
        getInternal(accountIdentifier, orgIdentifier, projectIdentifier);
    if (gitFullSyncConfigOptional.isPresent()) {
      GitFullSyncConfig gitFullSyncConfig = gitFullSyncConfigOptional.get();
      gitFullSyncConfig.setBaseBranch(dto.getBaseBranch());
      gitFullSyncConfig.setBranch(dto.getBranch());
      gitFullSyncConfig.setPrTitle(dto.getPrTitle());
      gitFullSyncConfig.setCreatePullRequest(dto.isCreatePullRequest());
      gitFullSyncConfig.setYamlGitConfigIdentifier(dto.getRepoIdentifier());
      gitFullSyncConfig.setTargetBranch(dto.getTargetBranch());
      gitFullSyncConfig.setNewBranch(dto.isNewBranch());
      gitFullSyncConfig.setRootFolder(dto.getRootFolder());
      return GitFullSyncConfigMapper.toDTO(gitFullSyncConfigRepository.save(gitFullSyncConfig));
    }
    throw new InvalidRequestException("No such configuration found, please check the scope.", WingsException.USER);
  }

  @Override
  public boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Optional<GitFullSyncConfig> gitFullSyncConfigOptional =
        getInternal(accountIdentifier, orgIdentifier, projectIdentifier);
    if (gitFullSyncConfigOptional.isPresent()) {
      gitFullSyncConfigRepository.delete(gitFullSyncConfigOptional.get());
      return true;
    }
    return false;
  }
}
