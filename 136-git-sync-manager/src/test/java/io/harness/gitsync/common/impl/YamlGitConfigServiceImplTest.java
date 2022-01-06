/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.gitsync.common.remote.YamlGitConfigMapper.toSetupGitSyncDTO;
import static io.harness.gitsync.common.remote.YamlGitConfigMapper.toYamlGitConfigDTO;
import static io.harness.rule.OwnerRule.ABHINAV;
import static io.harness.rule.OwnerRule.DEEPAK;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.gitsync.common.dtos.GitSyncConfigDTO;
import io.harness.gitsync.common.dtos.GitSyncFolderConfigDTO;
import io.harness.gitsync.common.events.GitSyncConfigChangeEventConstants;
import io.harness.gitsync.common.events.GitSyncConfigChangeEventType;
import io.harness.gitsync.common.events.GitSyncConfigSwitchType;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(DX)
public class YamlGitConfigServiceImplTest extends GitSyncTestBase {
  @Mock ConnectorService defaultConnectorService;
  @Inject YamlGitConfigServiceImpl yamlGitConfigService;
  @Mock Producer gitSyncConfigEventProducer;
  private final String ACCOUNT_ID = "ACCOUNT_ID";
  private final String ORG_ID = "ORG_ID";
  private final String PROJECT_ID = "PROJECT_ID";
  private final String IDENTIFIER = "ID";
  private final String IDENTIFIER_1 = "ID_1";
  private final String CONNECTOR_ID = "CONNECTOR_ID";
  private final String CONNECTOR_ID_1 = "CONNECTOR_ID_1";
  private final String REPO = "REPO";
  private final String BRANCH = "BRANCH";
  private final String ROOT_FOLDER = "ROOT_FOLDER/.harness/";
  private final String ROOT_FOLDER_ID = "ROOT_FOLDER_ID";
  private final String ROOT_FOLDER_1 = "ROOT_FOLDER_1";
  private final String ROOT_FOLDER_ID_1 = "ROOT_FOLDER_ID_1/.harness/";

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    GithubConnectorDTO githubConnector =
        GithubConnectorDTO.builder().apiAccess(GithubApiAccessDTO.builder().build()).build();
    ConnectorInfoDTO connectorInfo = ConnectorInfoDTO.builder().connectorConfig(githubConnector).build();
    when(defaultConnectorService.get(any(), any(), any(), any()))
        .thenReturn(Optional.of(ConnectorResponseDTO.builder().connector(connectorInfo).build()));
    FieldUtils.writeField(yamlGitConfigService, "connectorService", defaultConnectorService, true);
    FieldUtils.writeField(yamlGitConfigService, "gitSyncConfigEventProducer", gitSyncConfigEventProducer, true);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void test_save() {
    GitSyncFolderConfigDTO rootFolder =
        GitSyncFolderConfigDTO.builder().isDefault(true).rootFolder(ROOT_FOLDER).build();
    GitSyncConfigDTO gitSyncConfigDTO =
        buildGitSyncDTO(Collections.singletonList(rootFolder), CONNECTOR_ID, REPO, BRANCH, IDENTIFIER);
    GitSyncConfigDTO ret =
        toSetupGitSyncDTO(yamlGitConfigService.save(toYamlGitConfigDTO(gitSyncConfigDTO, ACCOUNT_ID)));
    assertThat(ret).isEqualTo(gitSyncConfigDTO);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_gitSyncEnabledEventIsSent() throws Exception {
    ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);

    GitSyncFolderConfigDTO rootFolder =
        GitSyncFolderConfigDTO.builder().isDefault(true).rootFolder(ROOT_FOLDER).build();
    GitSyncConfigDTO gitSyncConfigDTO =
        buildGitSyncDTO(Collections.singletonList(rootFolder), CONNECTOR_ID, REPO, BRANCH, IDENTIFIER);
    GitSyncConfigDTO ret =
        toSetupGitSyncDTO(yamlGitConfigService.save(toYamlGitConfigDTO(gitSyncConfigDTO, ACCOUNT_ID)));
    verify(gitSyncConfigEventProducer, times(1)).send(messageArgumentCaptor.capture());

    Message message = messageArgumentCaptor.getValue();
    final Map<String, String> metadataMap = message.getMetadataMap();
    assertThat(metadataMap.get("accountId")).isEqualTo(ACCOUNT_ID);
    assertThat(metadataMap.get(GitSyncConfigChangeEventConstants.EVENT_TYPE))
        .isEqualTo(GitSyncConfigChangeEventType.SAVE_EVENT.name());
    assertThat(metadataMap.get(GitSyncConfigChangeEventConstants.CONFIG_SWITCH_TYPE))
        .isEqualTo(GitSyncConfigSwitchType.ENABLED.name());
    final ByteString data = message.getData();
    final EntityScopeInfo entityScopeInfo = EntityScopeInfo.parseFrom(data);
    assertThat(entityScopeInfo.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(entityScopeInfo.getOrgId().getValue()).isEqualTo(ORG_ID);
    assertThat(entityScopeInfo.getProjectId().getValue()).isEqualTo(PROJECT_ID);
    assertThat(ret).isEqualTo(gitSyncConfigDTO);
  }

  private GitSyncConfigDTO buildGitSyncDTO(
      List<GitSyncFolderConfigDTO> rootFolder, String connectorId, String repo, String branch, String identifier) {
    return GitSyncConfigDTO.builder()
        .orgIdentifier(ORG_ID)
        .projectIdentifier(PROJECT_ID)
        .repo(repo)
        .branch(branch)
        .gitConnectorRef(connectorId)
        .identifier(identifier)
        .gitSyncFolderConfigDTOs(rootFolder)
        .name(repo)
        .build();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testConnectorUpdate() {
    GitSyncFolderConfigDTO rootFolder =
        GitSyncFolderConfigDTO.builder().isDefault(true).rootFolder(ROOT_FOLDER).build();
    GitSyncConfigDTO gitSyncConfigDTO =
        saveYamlGitConfig(Collections.singletonList(rootFolder), CONNECTOR_ID, REPO, BRANCH, IDENTIFIER);
    gitSyncConfigDTO.setGitConnectorRef(CONNECTOR_ID_1);
    GitSyncConfigDTO ret =
        toSetupGitSyncDTO(yamlGitConfigService.update(toYamlGitConfigDTO(gitSyncConfigDTO, ACCOUNT_ID)));
    assertThat(ret.getGitConnectorRef()).isEqualTo(CONNECTOR_ID_1);
  }

  private GitSyncFolderConfigDTO getDefault(GitSyncConfigDTO gitSyncConfigDTOS) {
    List<GitSyncFolderConfigDTO> gitSyncFolderConfigDTOs = gitSyncConfigDTOS.getGitSyncFolderConfigDTOs();
    Optional<GitSyncFolderConfigDTO> defaultGitSync =
        gitSyncFolderConfigDTOs.stream().filter(GitSyncFolderConfigDTO::getIsDefault).findFirst();
    return defaultGitSync.orElse(null);
  }

  private GitSyncConfigDTO saveYamlGitConfig(
      List<GitSyncFolderConfigDTO> rootFolder, String connectorId, String repo, String branch, String identifier) {
    GitSyncConfigDTO gitSyncConfigDTO = buildGitSyncDTO(rootFolder, connectorId, repo, branch, identifier);
    return toSetupGitSyncDTO(yamlGitConfigService.save(toYamlGitConfigDTO(gitSyncConfigDTO, ACCOUNT_ID)));
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testValidateThatHarnessStringComesOnceWithValidInput() {
    List<YamlGitConfigDTO.RootFolder> rootFolders =
        Arrays.asList(YamlGitConfigDTO.RootFolder.builder().rootFolder(ROOT_FOLDER).build(),
            YamlGitConfigDTO.RootFolder.builder().rootFolder(ROOT_FOLDER_1).build());
    YamlGitConfigDTO yamlGitConfigDTO = YamlGitConfigDTO.builder().rootFolders(rootFolders).build();
    yamlGitConfigService.validateThatHarnessStringComesOnce(yamlGitConfigDTO);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testValidateThatHarnessStringComesOnceWithInValidInput() {
    List<YamlGitConfigDTO.RootFolder> rootFolders =
        Arrays.asList(YamlGitConfigDTO.RootFolder.builder().rootFolder("/src/.harness/src1/.harness").build(),
            YamlGitConfigDTO.RootFolder.builder().rootFolder(ROOT_FOLDER_1).build());
    YamlGitConfigDTO yamlGitConfigDTO = YamlGitConfigDTO.builder().rootFolders(rootFolders).build();
    assertThatThrownBy(() -> yamlGitConfigService.validateThatHarnessStringComesOnce(yamlGitConfigDTO))
        .isInstanceOf(InvalidRequestException.class);
  }
}
