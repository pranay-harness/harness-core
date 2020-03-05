package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.VARDAN_BANSAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.EntityType;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.beans.yaml.GitFileChange;
import software.wings.exception.YamlProcessingException;
import software.wings.service.impl.yaml.gitdiff.GitChangeSetHandler;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.Arrays;
import java.util.Map;

public class GitChangeSetHandlerTest extends WingsBaseTest {
  @InjectMocks @Inject private GitChangeSetHandler gitChangeSetHandler;
  private String accountId = generateUuid();
  private String commitId = generateUuid();
  private String uuid = generateUuid();
  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_shouldIngestGitYamlChanges() {
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
                                   .branchName("branchName")
                                   .gitConnectorId(uuid)
                                   .enabled(Boolean.TRUE)
                                   .build())
                .build());
    assertThat(changeWithErrorMsgMap).isNotNull();
    assertThat(changeWithErrorMsgMap.values().isEmpty()).isEqualTo(true);
  }
}
