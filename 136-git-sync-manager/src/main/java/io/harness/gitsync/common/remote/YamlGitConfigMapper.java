package io.harness.gitsync.common.remote;

import static io.harness.encryption.ScopeHelper.getScope;

import static io.fabric8.utils.Strings.nullIfEmpty;

import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.encryption.Scope;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.common.beans.YamlGitConfig;
import io.harness.gitsync.common.dtos.GitSyncConfigDTO;
import io.harness.gitsync.common.dtos.GitSyncFolderConfigDTO;
import io.harness.gitsync.common.dtos.GitSyncFolderConfigDTO.GitSyncFolderConfigDTOBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.SneakyThrows;

public class YamlGitConfigMapper {
  public static final YamlGitConfig toYamlGitConfig(YamlGitConfigDTO yamlGitConfigDTO) {
    return YamlGitConfig.builder()
        .accountId(yamlGitConfigDTO.getAccountId())
        .organizationId(nullIfEmpty(yamlGitConfigDTO.getOrganizationId()))
        .uuid(nullIfEmpty(yamlGitConfigDTO.getIdentifier()))
        .projectId(nullIfEmpty(yamlGitConfigDTO.getProjectId()))
        .branch(yamlGitConfigDTO.getBranch())
        .repo(yamlGitConfigDTO.getRepo())
        .gitConnectorId(yamlGitConfigDTO.getGitConnectorId())
        .scope(yamlGitConfigDTO.getScope())
        .rootFolders(yamlGitConfigDTO.getRootFolders())
        .defaultRootFolder(yamlGitConfigDTO.getDefaultRootFolder())
        .build();
  }

  public static final YamlGitConfigDTO toYamlGitConfigDTO(YamlGitConfig yamlGitConfig) {
    return YamlGitConfigDTO.builder()
        .accountId(yamlGitConfig.getAccountId())
        .organizationId(nullIfEmpty(yamlGitConfig.getOrganizationId()))
        .identifier(nullIfEmpty(yamlGitConfig.getUuid()))
        .projectId(nullIfEmpty(yamlGitConfig.getProjectId()))
        .branch(yamlGitConfig.getBranch())
        .repo(yamlGitConfig.getRepo())
        .gitConnectorId(yamlGitConfig.getGitConnectorId())
        .scope(yamlGitConfig.getScope())
        .rootFolders(yamlGitConfig.getRootFolders())
        .defaultRootFolder(yamlGitConfig.getDefaultRootFolder())
        .build();
  }

  public static final YamlGitConfigDTO toYamlGitConfigDTO(GitSyncConfigDTO gitSyncConfigDTO) {
    Scope scope = getScope(
        gitSyncConfigDTO.getAccountId(), gitSyncConfigDTO.getOrganizationId(), gitSyncConfigDTO.getProjectId());
    return YamlGitConfigDTO.builder()
        .accountId(gitSyncConfigDTO.getAccountId())
        .organizationId(gitSyncConfigDTO.getOrganizationId())
        .projectId(gitSyncConfigDTO.getProjectId())
        .scope(scope)
        .rootFolders(toRootFolders(gitSyncConfigDTO.getGitSyncFolderConfigDTOs()))
        .defaultRootFolder(getDefaultRootFolder(gitSyncConfigDTO.getGitSyncFolderConfigDTOs()))
        .gitConnectorId(gitSyncConfigDTO.getGitConnectorId())
        .branch(gitSyncConfigDTO.getBranch())
        .repo(gitSyncConfigDTO.getRepo())
        .identifier(gitSyncConfigDTO.getIdentifier())
        .build();
  }

  private static List<YamlGitConfigDTO.RootFolder> toRootFolders(List<GitSyncFolderConfigDTO> gitSyncFolderConfigDTOS) {
    return gitSyncFolderConfigDTOS.stream().map(YamlGitConfigMapper::getRootFolders).collect(Collectors.toList());
  }

  private static YamlGitConfigDTO.RootFolder getDefaultRootFolder(
      List<GitSyncFolderConfigDTO> gitSyncFolderConfigDTOS) {
    Optional<GitSyncFolderConfigDTO> gitSyncFolderDTO =
        gitSyncFolderConfigDTOS.stream().filter(GitSyncFolderConfigDTO::getIsDefault).findFirst();
    return gitSyncFolderDTO.map(YamlGitConfigMapper::getRootFolders).orElse(null);
  }

  private static YamlGitConfigDTO.RootFolder getRootFolders(GitSyncFolderConfigDTO gitSyncFolderConfigDTO) {
    return YamlGitConfigDTO.RootFolder.builder()
        .enabled(gitSyncFolderConfigDTO.getEnabled())
        .identifier(gitSyncFolderConfigDTO.getIdentifier())
        .rootFolder(gitSyncFolderConfigDTO.getRootFolder())
        .build();
  }

  public static final GitSyncConfigDTO toSetupGitSyncDTO(YamlGitConfigDTO yamlGitConfig) {
    return GitSyncConfigDTO.builder()
        .accountId(yamlGitConfig.getAccountId())
        .identifier(yamlGitConfig.getIdentifier())
        .organizationId(yamlGitConfig.getOrganizationId())
        .projectId(yamlGitConfig.getProjectId())
        .gitConnectorId(yamlGitConfig.getGitConnectorId())
        .branch(yamlGitConfig.getBranch())
        .repo(yamlGitConfig.getRepo())
        .gitSyncFolderConfigDTOs(
            toGitSyncFolderDTO(yamlGitConfig.getRootFolders(), yamlGitConfig.getDefaultRootFolder()))
        .build();
  }

  private static List<GitSyncFolderConfigDTO> toGitSyncFolderDTO(
      List<YamlGitConfigDTO.RootFolder> rootFolders, YamlGitConfigDTO.RootFolder defaultRootFolder) {
    return rootFolders.stream()
        .map(rootFolder -> {
          GitSyncFolderConfigDTOBuilder gitSyncFolderDTOBuilder = GitSyncFolderConfigDTO.builder()
                                                                      .enabled(rootFolder.isEnabled())
                                                                      .identifier(rootFolder.getIdentifier())
                                                                      .isDefault(false)
                                                                      .rootFolder(rootFolder.getRootFolder());
          if (rootFolder.equals(defaultRootFolder)) {
            gitSyncFolderDTOBuilder.isDefault(true);
          }
          return gitSyncFolderDTOBuilder.build();
        })
        .collect(Collectors.toList());
  }

  @SneakyThrows
  static YamlGitConfigDTO applyUpdateToYamlGitConfigDTO(
      YamlGitConfigDTO yamlGitConfigDTO, YamlGitConfigDTO updateYamlGitConfigDTO) {
    if (!yamlGitConfigDTO.getIdentifier().equals(updateYamlGitConfigDTO.getIdentifier())) {
      throw new InvalidRequestException("Incorrect identifier of git sync for update.");
    }
    String jsonString = new ObjectMapper().writer().writeValueAsString(updateYamlGitConfigDTO);
    return new ObjectMapper().readerForUpdating(yamlGitConfigDTO).readValue(jsonString);
  }
}
