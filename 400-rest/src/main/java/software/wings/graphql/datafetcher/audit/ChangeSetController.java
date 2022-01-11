/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.audit;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;
import io.harness.exception.GraphQLException;
import io.harness.exception.WingsException;

import software.wings.audit.ApiKeyAuditDetails;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditSource;
import software.wings.audit.EntityAuditRecord;
import software.wings.audit.GitAuditDetails;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.type.audit.QLApiKeyChangeSet;
import software.wings.graphql.schema.type.audit.QLApiKeyChangeSet.QLApiKeyChangeSetBuilder;
import software.wings.graphql.schema.type.audit.QLChangeDetails;
import software.wings.graphql.schema.type.audit.QLChangeSet;
import software.wings.graphql.schema.type.audit.QLGenericChangeSet;
import software.wings.graphql.schema.type.audit.QLGenericChangeSet.QLGenericChangeSetBuilder;
import software.wings.graphql.schema.type.audit.QLGitChangeSet;
import software.wings.graphql.schema.type.audit.QLGitChangeSet.QLGitChangeSetBuilder;
import software.wings.graphql.schema.type.audit.QLRequestInfo;
import software.wings.graphql.schema.type.audit.QLUserChangeSet;
import software.wings.graphql.schema.type.audit.QLUserChangeSet.QLUserChangeSetBuilder;
import software.wings.service.impl.yaml.gitdiff.gitaudit.YamlAuditRecordGenerationUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Singleton
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class ChangeSetController {
  @Inject YamlAuditRecordGenerationUtils yamlAuditRecordGenerationUtils;
  public QLChangeSet populateChangeSet(@NotNull AuditHeader audit) {
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
      case APIKEY:
        final QLApiKeyChangeSetBuilder apiKeyChangeSetBuilder = QLApiKeyChangeSet.builder();
        populateApiKeyChangeSet(audit, apiKeyChangeSetBuilder);
        return apiKeyChangeSetBuilder.build();
      case NONE:
        final QLGenericChangeSetBuilder genericChangeSetBuilder = QLGenericChangeSet.builder();
        populateGenericChangeSet(audit, genericChangeSetBuilder);
        return genericChangeSetBuilder.build();
      default:
        throw new GraphQLException(
            String.format("Unsupported changeSet type found for changeSetId: %s", audit.getUuid()),
            WingsException.USER_SRE);
    }
  }

  /**
   * Determines the source of Audit creation -> from UI - through user / git / from apiKey through GraphQL
   * Audit source NONE is for events like LOGIN etc.
   * @param audit
   * @return
   */
  private AuditSource getAuditSource(@NotNull AuditHeader audit) {
    if (yamlAuditRecordGenerationUtils.verifyGitAsSourceForAuditTrail(audit.getCreatedBy())) {
      return AuditSource.GIT;
    } else if (verifyApiKeyAsSourceForAuditTrail(audit.getApiKeyAuditDetails())) {
      return AuditSource.APIKEY;
    } else if (verifyUserAsSourceForAuditTrail(audit.getCreatedBy())) {
      return AuditSource.USER;
    } else {
      return AuditSource.NONE;
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

  private void populateApiKeyChangeSet(@NotNull AuditHeader audit, QLApiKeyChangeSetBuilder builder) {
    builder.id(audit.getUuid())
        .changes(populateChangeSetWithDetails(audit.getEntityAuditRecords()))
        .triggeredAt(audit.getLastUpdatedAt())
        .request(populateChangeSetWithRequestInfo(audit))
        .failureStatusMsg(audit.getFailureStatusMsg())
        .apiKeyId(audit.getApiKeyAuditDetails() != null ? audit.getApiKeyAuditDetails().getApiKeyId() : null);
  }

  private void populateGenericChangeSet(@NotNull AuditHeader audit, QLGenericChangeSetBuilder builder) {
    builder.id(audit.getUuid())
        .changes(populateChangeSetWithDetails(audit.getEntityAuditRecords()))
        .triggeredAt(audit.getLastUpdatedAt())
        .request(populateChangeSetWithRequestInfo(audit))
        .failureStatusMsg(audit.getFailureStatusMsg());
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

  private boolean verifyApiKeyAsSourceForAuditTrail(ApiKeyAuditDetails apiKeyAuditDetails) {
    return apiKeyAuditDetails != null && apiKeyAuditDetails.getApiKeyId() != null;
  }

  private boolean verifyUserAsSourceForAuditTrail(EmbeddedUser user) {
    return user != null && user.getEmail() != null && user.getName() != null && user.getUuid() != null;
  }
}
