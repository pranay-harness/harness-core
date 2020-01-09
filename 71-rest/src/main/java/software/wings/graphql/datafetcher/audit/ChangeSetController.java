package software.wings.graphql.datafetcher.audit;

import com.google.inject.Singleton;

import io.harness.beans.EmbeddedUser;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditSource;
import software.wings.audit.EntityAuditRecord;
import software.wings.audit.GitAuditDetails;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.type.audit.QLChangeDetails;
import software.wings.graphql.schema.type.audit.QLChangeSet;
import software.wings.graphql.schema.type.audit.QLGitChangeSet;
import software.wings.graphql.schema.type.audit.QLGitChangeSet.QLGitChangeSetBuilder;
import software.wings.graphql.schema.type.audit.QLRequestInfo;
import software.wings.graphql.schema.type.audit.QLUserChangeSet;
import software.wings.graphql.schema.type.audit.QLUserChangeSet.QLUserChangeSetBuilder;

import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Singleton
public class ChangeSetController {
  public QLChangeSet populateChangeSet(@NotNull AuditHeader audit) {
    // TODO  how to determine which implementation of ChangeSet interface to return (which source?)
    AuditSource type = getAuditSource(audit);
    switch (type) {
      case USER:
        final QLUserChangeSetBuilder userChangeSetBuilder = QLUserChangeSet.builder();
        populateUserChangeSet(audit, userChangeSetBuilder);
        return userChangeSetBuilder.build();
      case GIT:
        final QLGitChangeSetBuilder gitChangeSetBuilder = QLGitChangeSet.builder();
        populateGitChangeSet(audit, gitChangeSetBuilder);
        return gitChangeSetBuilder.build();
      default:
        return null;
    }
  }

  private AuditSource getAuditSource(@NotNull AuditHeader audit) {
    EmbeddedUser createdBy = audit.getCreatedBy();
    if (createdBy != null && "GIT_SYNC".equals(createdBy.getName()) && "GIT".equals(createdBy.getUuid())) {
      return AuditSource.GIT;
    } else {
      return AuditSource.USER;
    }
  }

  private void populateUserChangeSet(@NotNull AuditHeader audit, QLUserChangeSetBuilder builder) {
    builder.id(audit.getUuid())
        .changes(populateChangeSetWithDetails(audit.getEntityAuditRecords()))
        .triggeredAt(audit.getLastUpdatedAt())
        .request(populateChangeSetWithRequestInfo(audit))
        .failureStatusMsg(audit.getFailureStatusMsg())
        .triggeredBy(UserController.populateUser(audit.getCreatedBy()));
  }

  private void populateGitChangeSet(@NotNull AuditHeader audit, QLGitChangeSetBuilder builder) {
    builder.id(audit.getUuid())
        .changes(populateChangeSetWithDetails(audit.getEntityAuditRecords()))
        .triggeredAt(audit.getLastUpdatedAt())
        .request(populateChangeSetWithRequestInfo(audit))
        .failureStatusMsg(audit.getFailureStatusMsg());
    populateGitChangeSetWithGitAuditDetails(builder, audit.getGitAuditDetails());
  }

  private void populateGitChangeSetWithGitAuditDetails(QLGitChangeSetBuilder builder, GitAuditDetails gitAuditDetails) {
    if (gitAuditDetails == null) {
      return;
    }
    builder.author(gitAuditDetails.getAuthor())
        .gitCommitId(gitAuditDetails.getGitCommitId())
        .repoUrl(gitAuditDetails.getRepoUrl());
  }

  private QLRequestInfo populateChangeSetWithRequestInfo(AuditHeader audit) {
    if (audit == null) {
      return null;
    }
    return QLRequestInfo.builder()
        .url(audit.getUrl())
        .resourcePath(audit.getResourcePath())
        .requestMethod(audit.getRequestMethod() != null ? audit.getRequestMethod().toString() : null)
        .responseStatusCode(audit.getResponseStatusCode())
        .remoteIpAddress(audit.getRemoteIpAddress())
        .build();
  }

  private List<QLChangeDetails> populateChangeSetWithDetails(List<EntityAuditRecord> entityAuditRecords) {
    if (entityAuditRecords == null) {
      return null;
    }

    return entityAuditRecords.stream()
        .map(entityAuditRecord
            -> QLChangeDetails.builder()
                   .resourceId(entityAuditRecord.getEntityId())
                   .resourceType(entityAuditRecord.getEntityType())
                   .resourceName(entityAuditRecord.getEntityName())
                   .operationType(entityAuditRecord.getOperationType())
                   .failure(entityAuditRecord.isFailure())
                   .appId(entityAuditRecord.getAppId())
                   .appName(entityAuditRecord.getAppName())
                   .parentResourceId(entityAuditRecord.getAffectedResourceId())
                   .parentResourceName(entityAuditRecord.getAffectedResourceName())
                   .parentResourceType(entityAuditRecord.getAffectedResourceType())
                   .parentResourceOperation(entityAuditRecord.getAffectedResourceOperation())
                   .createdAt(entityAuditRecord.getCreatedAt())
                   .build())
        .collect(Collectors.toList());
  }
}
