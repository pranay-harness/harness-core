/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.gitdiff;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.VARDAN_BANSAL;

import static software.wings.utils.WingsTestConstants.REPOSITORY_NAME;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.git.model.ChangeType;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.EntityType;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.beans.yaml.GitFileChange;
import software.wings.exception.YamlProcessingException;
import software.wings.yaml.gitSync.YamlGitConfig;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Spy;

public class GitChangeSetHandlerTest extends WingsBaseTest {
  @InjectMocks @Inject @Spy private GitChangeSetHandler gitChangeSetHandler;
  private String accountId = generateUuid();
  private String commitId = generateUuid();
  private String uuid = generateUuid();

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_shouldIngestGitYamlChanges() {
    String branchName = "branchName";
    final Map<String, YamlProcessingException.ChangeWithErrorMsg> changeWithErrorMsgMap =
        gitChangeSetHandler.ingestGitYamlChangs(accountId,
            GitDiffResult.builder()
                .branch("branch")
                .commitId(commitId)
                .repoName("repoName")
                .gitFileChanges(Arrays.asList(GitFileChange.Builder.aGitFileChange()
                                                  .withChangeType(ChangeType.ADD)
                                                  .withAccountId(accountId)
                                                  .withCommitId(commitId)
                                                  .withFilePath("filePath.yaml")
                                                  .withSyncFromGit(Boolean.TRUE)
                                                  .build()))
                .yamlGitConfig(YamlGitConfig.builder()
                                   .accountId(accountId)
                                   .entityId(accountId)
                                   .entityType(EntityType.APPLICATION)
                                   .branchName(branchName)
                                   .gitConnectorId(uuid)
                                   .enabled(Boolean.TRUE)
                                   .repositoryName(REPOSITORY_NAME)
                                   .build())
                .build());
    assertThat(changeWithErrorMsgMap).isNotNull();
    assertThat(changeWithErrorMsgMap.values().isEmpty()).isEqualTo(true);
  }
}
