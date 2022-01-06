/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.gitsyncerror.utils;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorType;
import io.harness.gitsync.gitsyncerror.beans.GitToHarnessErrorDetails;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(DX)
public class GitSyncErrorUtilsTest extends CategoryTest {
  public static final String EMPTY_STR = "";

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testIsGitToHarnessSyncError() {
    GitSyncError gitToHarnessError = GitSyncError.builder().errorType(GitSyncErrorType.GIT_TO_HARNESS).build();
    boolean result = GitSyncErrorUtils.isGitToHarnessSyncError(gitToHarnessError);
    assertThat(result).isTrue();

    GitSyncError harnessToGitError = GitSyncError.builder().errorType(GitSyncErrorType.CONNECTIVITY_ISSUE).build();
    boolean resuultedExcpectedToBeFalse = GitSyncErrorUtils.isGitToHarnessSyncError(harnessToGitError);
    assertThat(resuultedExcpectedToBeFalse).isFalse();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetCommitIdOfError() {
    String gitCommitId = "gitCommitId";
    GitToHarnessErrorDetails gitToHarnessErrorDetails =
        GitToHarnessErrorDetails.builder().gitCommitId(gitCommitId).build();
    GitSyncError gitToHarnessError = GitSyncError.builder()
                                         .errorType(GitSyncErrorType.GIT_TO_HARNESS)
                                         .additionalErrorDetails(gitToHarnessErrorDetails)
                                         .build();
    String outputCommitId = GitSyncErrorUtils.getCommitIdOfError(gitToHarnessError);
    assertThat(outputCommitId).isEqualTo(gitCommitId);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testgetYamlContentOfError() {
    String yamlContent = "yamlContent";
    GitToHarnessErrorDetails gitToHarnessErrorDetails =
        GitToHarnessErrorDetails.builder().yamlContent(yamlContent).build();
    GitSyncError gitToHarnessError = GitSyncError.builder()
                                         .errorType(GitSyncErrorType.GIT_TO_HARNESS)
                                         .additionalErrorDetails(gitToHarnessErrorDetails)
                                         .build();
    String yamlContentOfGitToHarness = GitSyncErrorUtils.getYamlContentOfError(gitToHarnessError);
    assertThat(yamlContentOfGitToHarness).isEqualTo(yamlContent);
  }
}
