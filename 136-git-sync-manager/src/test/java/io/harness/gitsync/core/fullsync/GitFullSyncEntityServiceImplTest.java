/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.core.fullsync;

import static io.harness.rule.OwnerRule.PHOENIKX;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.EntityType;
import io.harness.Microservice;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.gitsync.FileChange;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo.SyncStatus;
import io.harness.gitsync.fullsync.dtos.GitFullSyncEntityInfoDTO;
import io.harness.gitsync.fullsync.dtos.GitFullSyncEntityInfoFilterDTO;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.entitydetail.EntityDetailProtoToRestMapper;
import io.harness.repositories.fullSync.GitFullSyncEntityRepository;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PL)
public class GitFullSyncEntityServiceImplTest extends GitSyncTestBase {
  public static final String ACCOUNT = "account";
  public static final String ORG = "org";
  public static final String PROJECT = "project";
  private GitFullSyncEntityServiceImpl gitFullSyncEntityService;
  @Inject private GitFullSyncEntityRepository gitFullSyncEntityRepository;
  @Inject private EntityDetailProtoToRestMapper entityDetailProtoToRestMapper;

  @Before
  public void setup() {
    gitFullSyncEntityService = new GitFullSyncEntityServiceImpl(gitFullSyncEntityRepository);
  }
  private String getStringValueFromProtoString(StringValue stringValue) {
    if (stringValue != null) {
      return stringValue.getValue();
    }
    return null;
  }

  private GitFullSyncEntityInfo getFullSyncEntityInfo(EntityScopeInfo entityScopeInfo, String messageId,
      Microservice microservice, FileChange entityForFullSync, SyncStatus syncStatus) {
    return GitFullSyncEntityInfo.builder()
        .accountIdentifier(entityScopeInfo.getAccountId())
        .filePath(entityForFullSync.getFilePath())
        .projectIdentifier(getStringValueFromProtoString(entityScopeInfo.getProjectId()))
        .orgIdentifier(getStringValueFromProtoString(entityScopeInfo.getOrgId()))
        .microservice(microservice.name())
        .messageId(messageId)
        .entityDetail(entityDetailProtoToRestMapper.createEntityDetailDTO(entityForFullSync.getEntityDetail()))
        .syncStatus(syncStatus.toString())
        .yamlGitConfigId(entityScopeInfo.getIdentifier())
        .retryCount(0)
        .build();
  }

  private void createFullSyncFile(String account, String org, String project, String filePath,
      EntityTypeProtoEnum entityTypeProtoEnum, String name, SyncStatus syncStatus) {
    EntityScopeInfo entityScopeInfo = EntityScopeInfo.newBuilder()
                                          .setAccountId(account)
                                          .setOrgId(StringValue.of(org))
                                          .setProjectId(StringValue.of(project))
                                          .build();
    FileChange fileChange =
        FileChange.newBuilder()
            .setFilePath(filePath)
            .setEntityDetail(EntityDetailProtoDTO.newBuilder().setType(entityTypeProtoEnum).setName(name).build())
            .build();
    GitFullSyncEntityInfo gitFullSyncEntityInfo =
        getFullSyncEntityInfo(entityScopeInfo, "messageId", random(Microservice.class), fileChange, syncStatus);
    gitFullSyncEntityService.save(gitFullSyncEntityInfo);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testListFullSyncFiles() {
    for (int i = 0; i < 6; i++) {
      createFullSyncFile(ACCOUNT, ORG, PROJECT, random(String.class), EntityTypeProtoEnum.CONNECTORS,
          random(String.class), SyncStatus.QUEUED);
    }
    for (int i = 0; i < 6; i++) {
      createFullSyncFile(ACCOUNT, ORG, PROJECT, random(String.class), EntityTypeProtoEnum.INPUT_SETS,
          random(String.class), SyncStatus.PUSHED);
    }
    PageResponse<GitFullSyncEntityInfoDTO> response =
        gitFullSyncEntityService.list(ACCOUNT, ORG, PROJECT, PageRequest.builder().pageIndex(0).pageSize(5).build(),
            null, GitFullSyncEntityInfoFilterDTO.builder().syncStatus(SyncStatus.QUEUED).build());
    assertThat(response.getContent()).isNotEmpty();
    assertThat(response.getTotalItems()).isEqualTo(6);
    assertThat(response.getPageItemCount()).isEqualTo(5);

    response =
        gitFullSyncEntityService.list(ACCOUNT, null, null, PageRequest.builder().pageIndex(0).pageSize(5).build(), null,
            GitFullSyncEntityInfoFilterDTO.builder().syncStatus(SyncStatus.QUEUED).build());
    assertThat(response.getContent()).isEmpty();

    response = gitFullSyncEntityService.list(ACCOUNT, ORG, PROJECT,
        PageRequest.builder().pageIndex(0).pageSize(5).build(), null,
        GitFullSyncEntityInfoFilterDTO.builder()
            .syncStatus(SyncStatus.PUSHED)
            .entityType(EntityType.INPUT_SETS)
            .build());
    assertThat(response.getContent()).hasSize(5);
    assertThat(response.getTotalItems()).isEqualTo(6);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testCountOfFilesWithFilter() {
    for (int i = 0; i < 6; i++) {
      createFullSyncFile(ACCOUNT, ORG, PROJECT, random(String.class), EntityTypeProtoEnum.CONNECTORS,
          random(String.class), SyncStatus.QUEUED);
    }
    for (int i = 0; i < 6; i++) {
      createFullSyncFile(ACCOUNT, ORG, PROJECT, random(String.class), EntityTypeProtoEnum.INPUT_SETS,
          random(String.class), SyncStatus.PUSHED);
    }

    long count =
        gitFullSyncEntityService.count(ACCOUNT, ORG, PROJECT, GitFullSyncEntityInfoFilterDTO.builder().build());
    assertThat(count).isEqualTo(12);

    count = gitFullSyncEntityService.count(ACCOUNT, null, null, GitFullSyncEntityInfoFilterDTO.builder().build());
    assertThat(count).isEqualTo(0);

    count = gitFullSyncEntityService.count(
        ACCOUNT, ORG, PROJECT, GitFullSyncEntityInfoFilterDTO.builder().entityType(EntityType.CONNECTORS).build());
    assertThat(count).isEqualTo(6);

    count = gitFullSyncEntityService.count(
        ACCOUNT, ORG, PROJECT, GitFullSyncEntityInfoFilterDTO.builder().syncStatus(SyncStatus.PUSHED).build());
    assertThat(count).isEqualTo(6);

    count = gitFullSyncEntityService.count(ACCOUNT, ORG, PROJECT,
        GitFullSyncEntityInfoFilterDTO.builder()
            .syncStatus(SyncStatus.PUSHED)
            .entityType(EntityType.INPUT_SETS)
            .build());
    assertThat(count).isEqualTo(6);
  }
}
