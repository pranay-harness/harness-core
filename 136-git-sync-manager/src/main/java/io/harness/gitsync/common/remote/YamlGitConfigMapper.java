package io.harness.gitsync.common.remote;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.encryption.ScopeHelper.getScope;

import static io.fabric8.utils.Strings.nullIfEmpty;
import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.encryption.Scope;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.common.beans.YamlGitConfig;
import io.harness.gitsync.common.dtos.GitSyncConfigDTO;
import io.harness.gitsync.common.dtos.GitSyncFolderConfigDTO;
import io.harness.gitsync.common.dtos.GitSyncFolderConfigDTO.GitSyncFolderConfigDTOBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.SneakyThrows;

@OwnedBy(DX)
public class YamlGitConfigMapper {
  public static final YamlGitConfig toYamlGitConfig(YamlGitConfigDTO yamlGitConfigDTO, String accountId) {
    return YamlGitConfig.builder()
        .accountId(yamlGitConfigDTO.getAccountIdentifier())
        .orgIdentifier(nullIfEmpty(yamlGitConfigDTO.getOrganizationIdentifier()))
        .identifier(nullIfEmpty(yamlGitConfigDTO.getIdentifier()))
        .name(yamlGitConfigDTO.getName())
        .projectIdentifier(nullIfEmpty(yamlGitConfigDTO.getProjectIdentifier()))
        .branch(yamlGitConfigDTO.getBranch())
        .repo(yamlGitConfigDTO.getRepo())
        .gitConnectorRef(yamlGitConfigDTO.getGitConnectorRef())
        .scope(yamlGitConfigDTO.getScope())
        .rootFolders(yamlGitConfigDTO.getRootFolders())
        .defaultRootFolder(yamlGitConfigDTO.getDefaultRootFolder())
        .accountId(accountId)
        .gitConnectorType(yamlGitConfigDTO.getGitConnectorType())
        .build();
  }

  public static final YamlGitConfigDTO toYamlGitConfigDTO(YamlGitConfig yamlGitConfig) {
    final boolean executeOnDelegate =
        yamlGitConfig != null && yamlGitConfig.getExecuteOnDelegate() != null && yamlGitConfig.getExecuteOnDelegate();
    return YamlGitConfigDTO.builder()
        .accountIdentifier(yamlGitConfig.getAccountId())
        .organizationIdentifier(nullIfEmpty(yamlGitConfig.getOrgIdentifier()))
        .identifier(nullIfEmpty(yamlGitConfig.getIdentifier()))
        .name(yamlGitConfig.getName())
        .projectIdentifier(nullIfEmpty(yamlGitConfig.getProjectIdentifier()))
        .branch(yamlGitConfig.getBranch())
        .repo(yamlGitConfig.getRepo())
        .gitConnectorRef(yamlGitConfig.getGitConnectorRef())
        .scope(yamlGitConfig.getScope())
        .rootFolders(yamlGitConfig.getRootFolders())
        .defaultRootFolder(yamlGitConfig.getDefaultRootFolder())
        .gitConnectorType(yamlGitConfig.getGitConnectorType())
        .executeOnDelegate(executeOnDelegate)
        .build();
  }

  public static final YamlGitConfigDTO toYamlGitConfigDTO(GitSyncConfigDTO gitSyncConfigDTO, String accountIdentifier) {
    Scope scope =
        getScope(accountIdentifier, gitSyncConfigDTO.getOrgIdentifier(), gitSyncConfigDTO.getProjectIdentifier());
    return YamlGitConfigDTO.builder()
        .accountIdentifier(accountIdentifier)
        .organizationIdentifier(gitSyncConfigDTO.getOrgIdentifier())
        .projectIdentifier(gitSyncConfigDTO.getProjectIdentifier())
        .scope(scope)
        .rootFolders(toRootFolders(gitSyncConfigDTO.getGitSyncFolderConfigDTOs()))
        .defaultRootFolder(getDefaultRootFolder(gitSyncConfigDTO.getGitSyncFolderConfigDTOs()))
        .gitConnectorRef(gitSyncConfigDTO.getGitConnectorRef())
        .branch(gitSyncConfigDTO.getBranch())
        .repo(gitSyncConfigDTO.getRepo())
        .name(gitSyncConfigDTO.getName())
        .identifier(gitSyncConfigDTO.getIdentifier())
        .gitConnectorType(gitSyncConfigDTO.getGitConnectorType())
        .build();
  }

  private static List<YamlGitConfigDTO.RootFolder> toRootFolders(List<GitSyncFolderConfigDTO> gitSyncFolderConfigDTOS) {
    if (isEmpty(gitSyncFolderConfigDTOS)) {
      return null;
    }
    return gitSyncFolderConfigDTOS.stream().map(YamlGitConfigMapper::getRootFolders).collect(toList());
  }

  private static YamlGitConfigDTO.RootFolder getDefaultRootFolder(
      List<GitSyncFolderConfigDTO> gitSyncFolderConfigDTOS) {
    if (isEmpty(gitSyncFolderConfigDTOS)) {
      return null;
    }
    List<GitSyncFolderConfigDTO> gitSyncFolderDTOList =
        gitSyncFolderConfigDTOS.stream().filter(GitSyncFolderConfigDTO::getIsDefault).collect(toList());
    if (gitSyncFolderDTOList.size() > 1) {
      throw new InvalidRequestException("The repo config cannot have more than one default root folders");
    }
    if (isEmpty(gitSyncFolderDTOList)) {
      return null;
    }
    GitSyncFolderConfigDTO gitSyncFolderDTO = gitSyncFolderDTOList.get(0);
    return getRootFolders(gitSyncFolderDTO);
  }

  private static YamlGitConfigDTO.RootFolder getRootFolders(GitSyncFolderConfigDTO gitSyncFolderConfigDTO) {
    return YamlGitConfigDTO.RootFolder.builder().rootFolder(gitSyncFolderConfigDTO.getRootFolder()).build();
  }

  public static final GitSyncConfigDTO toSetupGitSyncDTO(YamlGitConfigDTO yamlGitConfig) {
    return GitSyncConfigDTO.builder()
        .identifier(yamlGitConfig.getIdentifier())
        .name(yamlGitConfig.getName())
        .orgIdentifier(yamlGitConfig.getOrganizationIdentifier())
        .projectIdentifier(yamlGitConfig.getProjectIdentifier())
        .gitConnectorRef(yamlGitConfig.getGitConnectorRef())
        .branch(yamlGitConfig.getBranch())
        .repo(yamlGitConfig.getRepo())
        .gitSyncFolderConfigDTOs(
            toGitSyncFolderDTO(yamlGitConfig.getRootFolders(), yamlGitConfig.getDefaultRootFolder()))
        .gitConnectorType(yamlGitConfig.getGitConnectorType())
        .build();
  }

  private static List<GitSyncFolderConfigDTO> toGitSyncFolderDTO(
      List<YamlGitConfigDTO.RootFolder> rootFolders, YamlGitConfigDTO.RootFolder defaultRootFolder) {
    if (isEmpty(rootFolders)) {
      return null;
    }
    return rootFolders.stream()
        .map(rootFolder -> {
          GitSyncFolderConfigDTOBuilder gitSyncFolderDTOBuilder =
              GitSyncFolderConfigDTO.builder().isDefault(false).rootFolder(rootFolder.getRootFolder());
          if (rootFolder.equals(defaultRootFolder)) {
            gitSyncFolderDTOBuilder.isDefault(true);
          }
          return gitSyncFolderDTOBuilder.build();
        })
        .collect(toList());
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
