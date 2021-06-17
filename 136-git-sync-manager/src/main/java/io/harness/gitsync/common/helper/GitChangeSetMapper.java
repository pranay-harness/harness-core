package io.harness.gitsync.common.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.EntityType;
import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.ChangeSet;
import io.harness.gitsync.YamlGitConfigInfo;
import io.harness.gitsync.common.beans.GitToHarnessFileProcessingRequest;
import io.harness.gitsync.common.dtos.GitFileChangeDTO;
import io.harness.ng.core.event.EntityToEntityProtoHelper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.protobuf.StringValue;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(DX)
@UtilityClass
@Slf4j
public class GitChangeSetMapper {
  ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

  public List<ChangeSet> toChangeSetList(List<GitToHarnessFileProcessingRequest> fileContentsList, String accountId,
      List<YamlGitConfigDTO> yamlGitConfigDTOs) {
    return emptyIfNull(fileContentsList)
        .stream()
        .map(fileProcessingRequest
            -> mapToChangeSet(fileProcessingRequest.getFileDetails(), accountId, fileProcessingRequest.getChangeType(),
                yamlGitConfigDTOs))
        .filter(Objects::nonNull)
        .collect(toList());
  }

  private ChangeSet mapToChangeSet(
      GitFileChangeDTO fileContent, String accountId, ChangeType changeType, List<YamlGitConfigDTO> yamlGitConfigDTOs) {
    EntityType entityType = GitSyncUtils.getEntityTypeFromYaml(fileContent.getContent());
    ChangeSet.Builder builder = ChangeSet.newBuilder()
                                    .setAccountId(accountId)
                                    .setChangeType(ChangeTypeMapper.toProto(changeType))
                                    .setEntityType(EntityToEntityProtoHelper.getEntityTypeFromProto(entityType))
                                    .setYaml(fileContent.getContent())
                                    .setFilePath(fileContent.getPath());
    if (isNotBlank(fileContent.getObjectId())) {
      builder.setObjectId(StringValue.of(fileContent.getObjectId()));
    }
    if (isNotBlank(fileContent.getCommitId())) {
      builder.setObjectId(StringValue.of(fileContent.getCommitId()));
    }
    return setYamlGitConfigInfoInChangeset(fileContent, accountId, yamlGitConfigDTOs, builder);
  }

  private ChangeSet setYamlGitConfigInfoInChangeset(GitFileChangeDTO fileContent, String accountId,
      List<YamlGitConfigDTO> yamlGitConfigDTOs, ChangeSet.Builder builder) {
    String orgIdentifier;
    String projectIdentifier;
    try {
      final JsonNode jsonNode = objectMapper.readTree(fileContent.getContent());
      projectIdentifier = jsonNode.get(0).get(NGCommonEntityConstants.PROJECT_KEY).asText();
      orgIdentifier = jsonNode.get(0).get(NGCommonEntityConstants.ORG_KEY).asText();
    } catch (Exception e) {
      log.error(
          "Ill formed yaml found. Filepath: [{}], Content[{}]", fileContent.getPath(), fileContent.getContent(), e);
      return null;
    }

    final Optional<YamlGitConfigDTO> yamlGitConfigDTO =
        getYamlGitConfigDTO(yamlGitConfigDTOs, orgIdentifier, projectIdentifier);

    if (!yamlGitConfigDTO.isPresent()) {
      return null;
    } else {
      YamlGitConfigDTO ygc = yamlGitConfigDTO.get();
      final YamlGitConfigInfo.Builder yamlGitConfigBuilder =
          YamlGitConfigInfo.newBuilder().setAccountId(accountId).setYamlGitConfigId(ygc.getIdentifier());
      if (isNotEmpty(projectIdentifier)) {
        yamlGitConfigBuilder.setYamlGitConfigProjectIdentifier(StringValue.of(projectIdentifier));
      }
      if (isNotEmpty(orgIdentifier)) {
        yamlGitConfigBuilder.setYamlGitConfigOrgIdentifier(StringValue.of(orgIdentifier));
      }
      return builder.setYamlGitConfigInfo(yamlGitConfigBuilder.build()).build();
    }
  }

  private Optional<YamlGitConfigDTO> getYamlGitConfigDTO(
      List<YamlGitConfigDTO> yamlGitConfigDTOs, String orgIdentifier, String projectIdentifier) {
    // If we don't have any yaml git config for the scope we skip changeset
    return yamlGitConfigDTOs.stream()
        .map(ygc -> {
          boolean matches = true;
          if (isNotEmpty(ygc.getProjectIdentifier())) {
            matches = ygc.getProjectIdentifier().equals(projectIdentifier);
          }
          if (!matches) {
            return null;
          }
          if (isNotEmpty(ygc.getOrganizationIdentifier())) {
            matches = ygc.getOrganizationIdentifier().equals(orgIdentifier);
          }
          if (!matches) {
            return null;
          }
          return ygc;
        })
        .filter(Objects::nonNull)
        .findFirst();
  }
}
