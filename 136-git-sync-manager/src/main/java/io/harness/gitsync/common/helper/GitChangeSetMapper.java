package io.harness.gitsync.common.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.ChangeSet;
import io.harness.gitsync.ChangeType;
import io.harness.gitsync.common.beans.GitToHarnessFileProcessingRequest;
import io.harness.ng.core.event.EntityToEntityProtoHelper;
import io.harness.product.ci.scm.proto.FileContent;

import com.google.protobuf.StringValue;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(DX)
@UtilityClass
@Slf4j
public class GitChangeSetMapper {
  public List<ChangeSet> toChangeSetList(List<GitToHarnessFileProcessingRequest> fileContentsList, String accountId) {
    return emptyIfNull(fileContentsList)
        .stream()
        .map(fileProcessingRequest
            -> mapToChangeSet(fileProcessingRequest.getFileDetails(), accountId, fileProcessingRequest.getChangeType()))
        .collect(toList());
  }

  private ChangeSet mapToChangeSet(FileContent fileContent, String accountId, ChangeType changeType) {
    EntityType entityType = GitSyncUtils.getEntityTypeFromYaml(fileContent.getContent());
    ChangeSet.Builder builder = ChangeSet.newBuilder()
                                    .setAccountId(accountId)
                                    .setChangeType(changeType)
                                    .setEntityType(EntityToEntityProtoHelper.getEntityTypeFromProto(entityType))
                                    .setId(generateUuid())
                                    .setObjectId(StringValue.of(fileContent.getBlobId()))
                                    .setYaml(fileContent.getContent())
                                    .setFilePath(fileContent.getPath());
    if (isNotBlank(fileContent.getBlobId())) {
      builder.setObjectId(StringValue.of(fileContent.getBlobId()));
    }
    if (isNotBlank(fileContent.getCommitId())) {
      builder.setObjectId(StringValue.of(fileContent.getCommitId()));
    }
    return builder.build();
  }
}
