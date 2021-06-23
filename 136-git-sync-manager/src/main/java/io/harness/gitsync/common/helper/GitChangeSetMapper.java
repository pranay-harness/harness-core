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
import io.harness.gitsync.common.dtos.ChangeSetWithYamlStatusDTO;
import io.harness.gitsync.common.dtos.GitFileChangeDTO;
import io.harness.ng.core.event.EntityToEntityProtoHelper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.StringValue;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(DX)
@UtilityClass
@Slf4j
public class GitChangeSetMapper {
  private ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

  public List<ChangeSetWithYamlStatusDTO> toChangeSetList(List<GitToHarnessFileProcessingRequest> fileContentsList,
      String accountId, List<YamlGitConfigDTO> yamlGitConfigDTOs, String changesetId) {
    return emptyIfNull(fileContentsList)
        .stream()
        .map(fileProcessingRequest
            -> mapToChangeSet(fileProcessingRequest.getFileDetails(), accountId, fileProcessingRequest.getChangeType(),
                yamlGitConfigDTOs, changesetId))
        .filter(Objects::nonNull)
        .collect(toList());
  }

  private ChangeSetWithYamlStatusDTO mapToChangeSet(GitFileChangeDTO fileContent, String accountId,
      ChangeType changeType, List<YamlGitConfigDTO> yamlGitConfigDTOs, String changesetId) {
    ChangeSet.Builder builder = ChangeSet.newBuilder()
                                    .setAccountId(accountId)
                                    .setChangeType(ChangeTypeMapper.toProto(changeType))
                                    .setYaml(fileContent.getContent())
                                    .setChangeSetId(changesetId)
                                    .setFilePath(fileContent.getPath());
    if (isNotBlank(fileContent.getObjectId())) {
      builder.setObjectId(StringValue.of(fileContent.getObjectId()));
    }
    EntityType entityType = null;
    try {
      entityType = GitSyncUtils.getEntityTypeFromYaml(fileContent.getContent());
    } catch (Exception ex) {
      log.error("Unknown entity type encountered in file {}", fileContent.getPath(), ex);
      return ChangeSetWithYamlStatusDTO.builder()
          .changeSet(builder.build())
          .yamlInputErrorType(ChangeSetWithYamlStatusDTO.YamlInputErrorType.INVALID_ENTITY_TYPE)
          .build();
    }

    builder.setEntityType(EntityToEntityProtoHelper.getEntityTypeFromProto(entityType));
    return setYamlGitConfigInfoInChangeset(fileContent, accountId, yamlGitConfigDTOs, builder);
  }

  private ChangeSetWithYamlStatusDTO setYamlGitConfigInfoInChangeset(GitFileChangeDTO fileContent, String accountId,
      List<YamlGitConfigDTO> yamlGitConfigDTOs, ChangeSet.Builder builder) {
    String orgIdentifier;
    String projectIdentifier;
    try {
      final JsonNode jsonNode = convertYamlToJsonNode(fileContent.getContent());
      projectIdentifier = getKeyInNode(jsonNode, NGCommonEntityConstants.PROJECT_KEY);
      orgIdentifier = getKeyInNode(jsonNode, NGCommonEntityConstants.ORG_KEY);
    } catch (Exception e) {
      log.error(
          "Ill formed yaml found. Filepath: [{}], Content[{}]", fileContent.getPath(), fileContent.getContent(), e);
      return ChangeSetWithYamlStatusDTO.builder()
          .changeSet(builder.build())
          .yamlInputErrorType(ChangeSetWithYamlStatusDTO.YamlInputErrorType.PROJECT_ORG_IDENTIFIER_MISSING)
          .build();
    }

    final Optional<YamlGitConfigDTO> yamlGitConfigDTO =
        getYamlGitConfigDTO(yamlGitConfigDTOs, orgIdentifier, projectIdentifier);

    if (!yamlGitConfigDTO.isPresent()) {
      return ChangeSetWithYamlStatusDTO.builder()
          .changeSet(builder.build())
          .yamlInputErrorType(ChangeSetWithYamlStatusDTO.YamlInputErrorType.YAML_FROM_NOT_GIT_SYNCED_PROJECT)
          .build();
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
      ChangeSet updatedChangeSet = builder.setYamlGitConfigInfo(yamlGitConfigBuilder.build()).build();
      return ChangeSetWithYamlStatusDTO.builder()
          .changeSet(updatedChangeSet)
          .yamlInputErrorType(ChangeSetWithYamlStatusDTO.YamlInputErrorType.NIL)
          .build();
    }
  }

  @VisibleForTesting
  static String getKeyInNode(JsonNode jsonNode, String key) {
    return jsonNode.fields().next().getValue().get(key).asText();
  }

  @VisibleForTesting
  JsonNode convertYamlToJsonNode(String yaml) throws IOException {
    return objectMapper.readTree(yaml);
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
