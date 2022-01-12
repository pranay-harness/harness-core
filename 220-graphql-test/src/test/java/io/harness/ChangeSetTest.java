/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.VARDAN_BANSAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.ChangeSetGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.rule.Owner;

import software.wings.audit.ApiKeyAuditDetails;
import software.wings.audit.AuditHeader;
import software.wings.audit.EntityAuditRecord;
import software.wings.audit.GitAuditDetails;
import software.wings.graphql.schema.type.QLPageInfo;
import software.wings.graphql.schema.type.QLUser;
import software.wings.graphql.schema.type.aggregation.audit.QLTimeUnit;
import software.wings.graphql.schema.type.audit.QLApiKeyChangeSet;
import software.wings.graphql.schema.type.audit.QLChangeDetails;
import software.wings.graphql.schema.type.audit.QLChangeSet;
import software.wings.graphql.schema.type.audit.QLGenericChangeSet;
import software.wings.graphql.schema.type.audit.QLGitChangeSet;
import software.wings.graphql.schema.type.audit.QLRequestInfo;
import software.wings.graphql.schema.type.audit.QLUserChangeSet;

import com.google.inject.Inject;
import java.util.List;
import lombok.Data;
import lombok.Singular;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ChangeSetTest extends GraphQLTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ChangeSetGenerator changeSetGenerator;
  final int timeSlot = 60 * 1000;

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryForUserChangeSet_withSpecificTimeRangeFilter() {
    String changeSetQueryPattern = $.GQL(/*
{
  audits(
    filters:{
      time: {
        specific:{
          from:%d
          to:%d
        }
      }
    }
    limit:%d, offset: 0){
    nodes{
      id
      triggeredAt
      request{
        url
        resourcePath
        requestMethod
        remoteIpAddress
        responseStatusCode
      }
      changes{
appId
appName
createdAt
failure
operationType
parentResourceId
parentResourceName
parentResourceOperation
parentResourceType
resourceId
resourceName
resourceType
      }
    }
    pageInfo{
      hasMore
      limit
      total
      offset
    }
  }
}
*/ ChangeSetTest.class);
    String query = String.format(
        changeSetQueryPattern, System.currentTimeMillis() - timeSlot, System.currentTimeMillis() + timeSlot, 1);
    verifyWithTimeFilter(query);
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryForUserChangeSet_withRelativeTimeRangeFilter() {
    String changeSetQueryPattern = $.GQL(/*
{
  audits(
    filters:{
      time: {
        relative:{
          timeUnit:%s
          noOfUnits:%d
        }
      }
    }
    limit:%d, offset: 0){
    nodes{
      id
      triggeredAt
      request{
        url
        resourcePath
        requestMethod
        remoteIpAddress
        responseStatusCode
      }
      changes{
appId
appName
createdAt
failure
operationType
parentResourceId
parentResourceName
parentResourceOperation
parentResourceType
resourceId
resourceName
resourceType
      }
    }
    pageInfo{
      hasMore
      limit
      total
      offset
    }
  }
}
*/ ChangeSetTest.class);
    String query = String.format(changeSetQueryPattern, QLTimeUnit.DAYS.toString(), 2, 1);
    verifyWithTimeFilter(query);
  }

  private void verifyWithTimeFilter(String query) {
    final Randomizer.Seed seed = new Randomizer.Seed(0);
    final OwnerManager.Owners owners = ownerManager.create();
    owners.add(EmbeddedUser.builder().uuid(generateUuid()).email("email").name("name").build());
    final AuditHeader auditHeader = changeSetGenerator.ensureUserChangeSetTest(seed, owners);

    QLChangeSetConnectionUserImplUser changeSetConnection =
        qlExecute(QLChangeSetConnectionUserImplUser.class, query, auditHeader.getAccountId());
    assertThat(changeSetConnection.getNodes().size()).isEqualTo(1);
    verifyPageInfo(changeSetConnection.getPageInfo());
    QLUserChangeSet userChangeSet = changeSetConnection.getNodes().get(0);
    verifyChangeSet(userChangeSet, auditHeader);
    // verify triggered by
    QLUser triggeredBy = userChangeSet.getTriggeredBy();
    if (triggeredBy != null) {
      assertThat(triggeredBy.getEmail()).isEqualTo(auditHeader.getCreatedBy().getEmail());
      assertThat(triggeredBy.getId()).isEqualTo(auditHeader.getCreatedBy().getUuid());
      assertThat(triggeredBy.getName()).isEqualTo(auditHeader.getCreatedBy().getName());
    }
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryForGitChangeSet() {
    final Randomizer.Seed seed = new Randomizer.Seed(0);
    final OwnerManager.Owners owners = ownerManager.create();
    owners.add(EmbeddedUser.builder().uuid(generateUuid()).email("email").name("name").build());
    final AuditHeader auditHeader = changeSetGenerator.ensureGitChangeSetTest(seed, owners);

    String changeSetQueryPattern = $.GQL(/*
    {
      audits(limit:1){
      nodes{
        id
        triggeredAt
        ... on GitChangeSet{
            author
          }
        }
      }
    }
    */ ChangeSetTest.class);

    String query = String.format(changeSetQueryPattern, 1);

    QLChangeSetConnectionGitImpl changeSetConnection =
        qlExecute(QLChangeSetConnectionGitImpl.class, query, auditHeader.getAccountId());
    assertThat(changeSetConnection.getNodes().size()).isEqualTo(1);
    verifyPageInfo(changeSetConnection.getPageInfo());
    QLGitChangeSet gitChangeSet = changeSetConnection.getNodes().get(0);
    verifyChangeSet(gitChangeSet, auditHeader);
    if (auditHeader.getGitAuditDetails() != null) {
      GitAuditDetails gitAuditDetails = auditHeader.getGitAuditDetails();
      assertThat(gitChangeSet.getAuthor()).isEqualTo(gitAuditDetails.getAuthor());
      assertThat(gitChangeSet.getGitCommitId()).isEqualTo(gitAuditDetails.getGitCommitId());
      assertThat(gitChangeSet.getRepoUrl()).isEqualTo(gitAuditDetails.getRepoUrl());
    }
  }

  private void verifyPageInfo(QLPageInfo pageInfo) {
    if (pageInfo == null) {
      return;
    }
    assertThat(pageInfo.getHasMore()).isEqualTo(false);
    assertThat(pageInfo.getLimit()).isEqualTo(1);
    assertThat(pageInfo.getOffset()).isEqualTo(0);
    assertThat(pageInfo.getTotal()).isEqualTo(1);
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryForApiKeyChangeSet() {
    final Randomizer.Seed seed = new Randomizer.Seed(0);
    final OwnerManager.Owners owners = ownerManager.create();
    owners.add(EmbeddedUser.builder().uuid(generateUuid()).email("email").name("name").build());
    final AuditHeader auditHeader = changeSetGenerator.ensureGitChangeSetTest(seed, owners);

    String changeSetQueryPattern = $.GQL(/*
    {
    audits(limit:1){
      nodes{
        id
        ... on ApiKeyChangeSet{
          apiKeyId
        }
      }
    }
  }
  */ ChangeSetTest.class);

    String query = String.format(changeSetQueryPattern, 1);

    QLChangeSetConnectionApiKeyImpl changeSetConnection =
        qlExecute(QLChangeSetConnectionApiKeyImpl.class, query, auditHeader.getAccountId());
    assertThat(changeSetConnection.getNodes().size()).isEqualTo(1);
    verifyPageInfo(changeSetConnection.getPageInfo());
    QLApiKeyChangeSet apiKeyChangeSet = changeSetConnection.getNodes().get(0);
    verifyChangeSet(apiKeyChangeSet, auditHeader);
    if (auditHeader.getApiKeyAuditDetails() != null) {
      ApiKeyAuditDetails c = auditHeader.getApiKeyAuditDetails();
      assertThat(apiKeyChangeSet.getApiKeyId()).isEqualTo(apiKeyChangeSet.getApiKeyId());
    }
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryForGenericChangeSet() {
    final Randomizer.Seed seed = new Randomizer.Seed(0);
    final OwnerManager.Owners owners = ownerManager.create();
    owners.add(EmbeddedUser.builder().uuid(generateUuid()).email("email").name("name").build());
    final AuditHeader auditHeader = changeSetGenerator.ensureGitChangeSetTest(seed, owners);

    String changeSetQueryPattern = $.GQL(/*
    {
    audits(limit:1){
      nodes{
        id
        ... on GenericChangeSet{
          triggeredAt
        }
      }
    }
  }
  */ ChangeSetTest.class);

    String query = String.format(changeSetQueryPattern, 1);

    QLChangeSetConnectionGenericImpl changeSetConnection =
        qlExecute(QLChangeSetConnectionGenericImpl.class, query, auditHeader.getAccountId());
    assertThat(changeSetConnection.getNodes().size()).isEqualTo(1);
    verifyPageInfo(changeSetConnection.getPageInfo());
    QLGenericChangeSet genericChangeSet = changeSetConnection.getNodes().get(0);
    verifyChangeSet(genericChangeSet, auditHeader);
  }

  /**
   * Verify common attributes of different implementations of QLChangeSet
   * @param changeSet
   * @param auditHeader
   */
  private void verifyChangeSet(QLChangeSet changeSet, AuditHeader auditHeader) {
    assertThat(changeSet.getId()).isEqualTo(auditHeader.getUuid());
    // request
    if (changeSet.getRequest() != null) {
      QLRequestInfo requestInfo = changeSet.getRequest();
      assertThat(requestInfo.getRemoteIpAddress()).isEqualTo(auditHeader.getRemoteIpAddress());
      assertThat(requestInfo.getRequestMethod()).isEqualTo(auditHeader.getRequestMethod().toString());
      assertThat(requestInfo.getResourcePath()).isEqualTo(auditHeader.getResourcePath());
      assertThat(requestInfo.getUrl()).isEqualTo(auditHeader.getUrl());
      assertThat(requestInfo.getResponseStatusCode()).isEqualTo(auditHeader.getResponseStatusCode());
    }
    // changes
    if (changeSet.getChanges() != null) {
      QLChangeDetails details = changeSet.getChanges().get(0);
      EntityAuditRecord auditRecord = auditHeader.getEntityAuditRecords().get(0);
      assertThat(details.getAppId()).isEqualTo(auditRecord.getAppId());
      assertThat(details.getAppName()).isEqualTo(auditRecord.getAppName());
      assertThat(details.getCreatedAt()).isEqualTo(auditRecord.getCreatedAt());
      assertThat(details.getFailure()).isEqualTo(auditRecord.isFailure());
      assertThat(details.getOperationType()).isEqualTo(auditRecord.getOperationType());
      assertThat(details.getParentResourceId()).isEqualTo(auditRecord.getAffectedResourceId());
      assertThat(details.getParentResourceName()).isEqualTo(auditRecord.getAffectedResourceName());
      assertThat(details.getParentResourceOperation()).isEqualTo(auditRecord.getAffectedResourceOperation());
      assertThat(details.getParentResourceType()).isEqualTo(auditRecord.getAffectedResourceType());
      assertThat(details.getResourceId()).isEqualTo(auditRecord.getEntityId());
      assertThat(details.getResourceName()).isEqualTo(auditRecord.getEntityName());
      assertThat(details.getResourceType()).isEqualTo(auditRecord.getEntityType());
    }
  }

  @Data
  private static class QLChangeSetConnectionUserImplUser {
    private QLPageInfo pageInfo;
    @Singular private List<QLUserChangeSet> nodes;
  }

  @Data
  private static class QLChangeSetConnectionGitImpl {
    private QLPageInfo pageInfo;
    @Singular private List<QLGitChangeSet> nodes;
  }

  @Data
  private static class QLChangeSetConnectionApiKeyImpl {
    private QLPageInfo pageInfo;
    @Singular private List<QLApiKeyChangeSet> nodes;
  }

  @Data
  private static class QLChangeSetConnectionGenericImpl {
    private QLPageInfo pageInfo;
    @Singular private List<QLGenericChangeSet> nodes;
  }
}
