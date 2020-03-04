package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.VARDAN_BANSAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.yaml.GitSyncServiceImpl;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.gitSync.GitFileActivity;

import java.util.Arrays;

public class GitSyncServiceImplTest extends WingsBaseTest {
  @InjectMocks @Inject private GitSyncServiceImpl gitSyncService;
  @Inject private WingsPersistence wingsPersistence;
  private String accountId = generateUuid();

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_shouldListErrors() {
    final GitSyncError gitSyncError1 = GitSyncError.builder()
                                           .gitCommitId("gitCommitId1")
                                           .yamlFilePath("yamlFilePath1")
                                           .yamlContent("yamlContent1")
                                           .accountId(accountId)
                                           .build();

    final GitSyncError gitSyncError2 = GitSyncError.builder()
                                           .gitCommitId("gitCommitId2")
                                           .yamlFilePath("yamlFilePath2")
                                           .yamlContent("yamlContent2")
                                           .accountId(accountId)
                                           .build();

    wingsPersistence.save(Arrays.asList(gitSyncError1, gitSyncError2));

    final PageRequest pageRequest = PageRequestBuilder.aPageRequest().withOffset("0").withLimit("2").build();

    final PageResponse<GitSyncError> errorList = gitSyncService.fetchErrors(pageRequest);
    assertThat(errorList.size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_discardGitSyncErrorsForGivenIds() {
    final GitSyncError gitSyncError = GitSyncError.builder()
                                          .gitCommitId("gitCommitId")
                                          .yamlFilePath("yamlFilePath")
                                          .yamlContent("yamlContent")
                                          .accountId(accountId)
                                          .build();

    wingsPersistence.save(gitSyncError);
    gitSyncService.updateGitSyncErrorStatus(Arrays.asList(gitSyncError), GitFileActivity.Status.DISCARDED, accountId);
    assertThat(wingsPersistence.get(GitSyncError.class, gitSyncError.getUuid())).isEqualTo(null);
  }
}
