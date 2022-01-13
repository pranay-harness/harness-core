/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.core.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.gitsync.common.YamlConstants.EXTENSION_SEPARATOR;
import static io.harness.gitsync.common.YamlConstants.PATH_DELIMITER;
import static io.harness.gitsync.common.YamlConstants.YAML_EXTENSION;
import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.git.model.DiffResult;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.core.service.GitCommitService;
import io.harness.gitsync.core.service.YamlService;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity;
import io.harness.gitsync.gitfileactivity.service.GitSyncService;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(DX)
public class GitChangeSetProcessorTest extends CategoryTest {
  @Mock private GitSyncService gitSyncService;
  @Mock private GitCommitService gitCommitService;
  @Mock private YamlGitConfigService yamlGitConfigService;
  @Mock private YamlService yamlService;

  @InjectMocks @Inject private GitChangeSetProcessor gitChangeSetProcessor;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void test_obtainValidGitFileChangesBasedOnKnownEntityTypes() {
    final GitFileChange gitFileChange = GitFileChange.builder().build();
    final String rootPath = "rootPath";
    final String rootPath1 = "rootPath1";
    final EntityType connector = EntityType.CONNECTORS;
    final EntityType pipelines = EntityType.PIPELINES;
    final String entityIdentifier = "id";
    final String filePathConnector = rootPath + PATH_DELIMITER + connector.getYamlName() + PATH_DELIMITER
        + entityIdentifier + EXTENSION_SEPARATOR + YAML_EXTENSION;
    final String filePathPipeline = rootPath1 + PATH_DELIMITER + pipelines.getYamlName() + PATH_DELIMITER
        + entityIdentifier + EXTENSION_SEPARATOR + YAML_EXTENSION;
    final String filePathUnknown = rootPath1 + PATH_DELIMITER + "random" + PATH_DELIMITER + entityIdentifier
        + EXTENSION_SEPARATOR + YAML_EXTENSION;

    final List<GitFileChange> gitFileChanges = gitChangeSetProcessor.obtainValidGitFileChangesBasedOnKnownEntityTypes(
        Arrays.asList(GitFileChange.builder().filePath(filePathConnector).build(),
            GitFileChange.builder().filePath(filePathPipeline).build(),
            GitFileChange.builder().filePath(filePathUnknown).build()),
        "accountId");

    assertThat(gitFileChanges.size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void test_applySyncFromGit() {
    final GitFileChange gitFileChange = GitFileChange.builder().build();
    gitChangeSetProcessor.applySyncFromGit(Collections.singletonList(gitFileChange));
    gitChangeSetProcessor.applySyncFromGit(null);
    assertThat(gitFileChange.isSyncFromGit()).isEqualTo(true);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void test_processGitChangeSet() {
    final String commitId = "commitId";
    final String accountId = "accountId";
    final String repo = "repo";
    final String branch = "branch";
    GitFileChange gitFileChange = GitFileChange.builder()
                                      .filePath("filePath/0")
                                      .accountId(accountId)
                                      .processingCommitId(commitId)
                                      .commitId(commitId)
                                      .changeFromAnotherCommit(true)
                                      .syncFromGit(true)
                                      .build();
    GitFileChange gitFileChange1 = GitFileChange.builder()
                                       .filePath("filePath/1")
                                       .accountId(accountId)
                                       .processingCommitId(commitId)
                                       .commitId(commitId)
                                       .build();
    GitFileChange gitFileChange2 = GitFileChange.builder()
                                       .filePath("filePath/2")
                                       .accountId(accountId)
                                       .processingCommitId(commitId)
                                       .commitId(commitId)
                                       .changeFromAnotherCommit(false)
                                       .syncFromGit(true)
                                       .build();
    GitFileChange gitFileChange3 = GitFileChange.builder()
                                       .filePath("filePath/" + EntityType.CONNECTORS.getYamlName() + "/abc.yaml")
                                       .accountId(accountId)
                                       .processingCommitId(commitId)
                                       .commitId(commitId)
                                       .changeFromAnotherCommit(false)
                                       .syncFromGit(true)
                                       .build();

    DiffResult diffResult =
        DiffResult.builder()
            .gitFileChanges(Arrays.asList(gitFileChange, gitFileChange1, gitFileChange2, gitFileChange3))
            .accountId(accountId)
            .branch(branch)
            .commitId(commitId)
            .repoName(repo)
            .commitMessage("msg")
            .commitTimeMs(123123l)
            .build();
    final YamlGitConfigDTO yamlGitConfigDTO =
        YamlGitConfigDTO.builder()
            .rootFolders(
                Collections.singletonList(YamlGitConfigDTO.RootFolder.builder().rootFolder("filePath").build()))
            .branch(branch)
            .accountIdentifier(accountId)
            .gitConnectorRef("id")
            .repo(repo)
            .build();
    doReturn(Collections.singletonList(yamlGitConfigDTO))
        .when(yamlGitConfigService)
        .getByConnectorRepoAndBranch("id", repo, branch, accountId);

    ArgumentCaptor<List> validFilesBasedOnYamlGitConfigFilterCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<List> gitDiffResultChangeSetCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<List> totalFilesCaptor = ArgumentCaptor.forClass(List.class);

    gitChangeSetProcessor.processGitChangeSet(accountId, diffResult, "id", repo, branch);

    verify(gitSyncService, times(1))
        .logActivityForGitOperation(totalFilesCaptor.capture(), eq(GitFileActivity.Status.QUEUED), eq(true), eq(false),
            any(), any(), any(), eq(yamlGitConfigDTO));

    verify(gitSyncService, times(1))
        .logActivityForSkippedFiles(validFilesBasedOnYamlGitConfigFilterCaptor.capture(),
            gitDiffResultChangeSetCaptor.capture(), eq("Root Folder not configured."), eq(accountId), eq(commitId));
  }
}
