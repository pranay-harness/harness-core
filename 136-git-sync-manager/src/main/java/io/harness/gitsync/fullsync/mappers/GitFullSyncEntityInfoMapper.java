package io.harness.gitsync.fullsync.mappers;

import io.harness.EntityType;
import io.harness.common.EntityReference;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo.SyncStatus;
import io.harness.gitsync.fullsync.dtos.GitFullSyncEntityInfoDTO;
import io.harness.ng.core.EntityDetail;

import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GitFullSyncEntityInfoMapper {
  private EntityType getEntityType(EntityDetail entityDetail) {
    return Optional.ofNullable(entityDetail).map(EntityDetail::getType).orElse(null);
  }

  private String getName(EntityDetail entityDetail) {
    return Optional.ofNullable(entityDetail).map(EntityDetail::getName).orElse(null);
  }

  private String getBranch(EntityDetail entityDetail) {
    return Optional.ofNullable(entityDetail)
        .map(EntityDetail::getEntityRef)
        .map(EntityReference::getBranch)
        .orElse(null);
  }

  private String getRepo(EntityDetail entityDetail) {
    return Optional.ofNullable(entityDetail)
        .map(EntityDetail::getEntityRef)
        .map(EntityReference::getRepoIdentifier)
        .orElse(null);
  }

  public static GitFullSyncEntityInfoDTO toDTO(@NotNull GitFullSyncEntityInfo gitFullSyncEntityInfo) {
    return GitFullSyncEntityInfoDTO.builder()
        .accountIdentifier(gitFullSyncEntityInfo.getAccountIdentifier())
        .orgIdentifier(gitFullSyncEntityInfo.getOrgIdentifier())
        .projectIdentifier(gitFullSyncEntityInfo.getProjectIdentifier())
        .entityType(getEntityType(gitFullSyncEntityInfo.getEntityDetail()))
        .errorMessages(gitFullSyncEntityInfo.getErrorMessage())
        .branch(getBranch(gitFullSyncEntityInfo.getEntityDetail()))
        .repo(getRepo(gitFullSyncEntityInfo.getEntityDetail()))
        .name(getName(gitFullSyncEntityInfo.getEntityDetail()))
        .filePath(gitFullSyncEntityInfo.getFilePath())
        .retryCount(gitFullSyncEntityInfo.getRetryCount())
        .syncStatus(SyncStatus.valueOf(gitFullSyncEntityInfo.getSyncStatus()))
        .build();
  }
}
