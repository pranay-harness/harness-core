/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.sync;

import static io.harness.rule.OwnerRule.DEEPAK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.errorhandling.GitToHarnessErrorDetails;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class GitSyncErrorUtilsTest extends CategoryTest {
  @InjectMocks @Inject GitSyncErrorUtils gitSyncErrorUtils;
  public static final String EMPTY_STR = "";
  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testIsGitToHarnessSyncError() {
    GitSyncError gitToHarnessError = GitSyncError.builder().gitSyncDirection("GIT_TO_HARNESS").build();
    boolean result = gitSyncErrorUtils.isGitToHarnessSyncError(gitToHarnessError);
    assertThat(result).isTrue();

    GitSyncError harnessToGitError = GitSyncError.builder().gitSyncDirection("HARNESS_TO_GIT").build();
    boolean resuultedExcpectedToBeFalse = gitSyncErrorUtils.isGitToHarnessSyncError(harnessToGitError);
    assertThat(resuultedExcpectedToBeFalse).isFalse();
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testGetCommitIdOfError() {
    String gitCommitId = "gitCommitId";
    GitToHarnessErrorDetails gitToHarnessErrorDetails =
        GitToHarnessErrorDetails.builder().gitCommitId(gitCommitId).build();
    GitSyncError gitToHarnessError = GitSyncError.builder()
                                         .gitSyncDirection("GIT_TO_HARNESS")
                                         .additionalErrorDetails(gitToHarnessErrorDetails)
                                         .build();
    String outputCommitId = gitSyncErrorUtils.getCommitIdOfError(gitToHarnessError);
    assertThat(outputCommitId).isEqualTo(gitCommitId);

    GitSyncError harnessToGitError = GitSyncError.builder().gitSyncDirection("HARNESS_TO_GIT").build();
    String commitIdOfHarnessToGit = gitSyncErrorUtils.getCommitIdOfError(harnessToGitError);
    assertThat(commitIdOfHarnessToGit).isEqualTo(EMPTY_STR);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testgetYamlContentOfError() {
    String yamlContent = "yamlContent";
    GitToHarnessErrorDetails gitToHarnessErrorDetails =
        GitToHarnessErrorDetails.builder().yamlContent(yamlContent).build();
    GitSyncError gitToHarnessError = GitSyncError.builder()
                                         .gitSyncDirection("GIT_TO_HARNESS")
                                         .additionalErrorDetails(gitToHarnessErrorDetails)
                                         .build();
    String yamlContentOfGitToHarness = gitSyncErrorUtils.getYamlContentOfError(gitToHarnessError);
    assertThat(yamlContentOfGitToHarness).isEqualTo(yamlContent);

    GitSyncError harnessToGitError = GitSyncError.builder().gitSyncDirection("HARNESS_TO_GIT").build();
    String YamlContentOfHarnessToGit = gitSyncErrorUtils.getCommitIdOfError(harnessToGitError);
    assertThat(YamlContentOfHarnessToGit).isEqualTo(EMPTY_STR);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testgetCommitTimeOfError() {
    Long commitTime = 123L;
    GitToHarnessErrorDetails gitToHarnessErrorDetails =
        GitToHarnessErrorDetails.builder().commitTime(commitTime).build();
    GitSyncError gitToHarnessError = GitSyncError.builder()
                                         .gitSyncDirection("GIT_TO_HARNESS")
                                         .additionalErrorDetails(gitToHarnessErrorDetails)
                                         .build();
    Long commitTimeOfGitToHarness = gitSyncErrorUtils.getCommitTimeOfError(gitToHarnessError);
    assertThat(commitTimeOfGitToHarness).isEqualTo(commitTime);

    GitSyncError harnessToGitError = GitSyncError.builder().gitSyncDirection("HARNESS_TO_GIT").build();
    Long commitTimeOfHarnessToGit = gitSyncErrorUtils.getCommitTimeOfError(harnessToGitError);
    assertThat(commitTimeOfHarnessToGit).isEqualTo(0L);
  }
}
