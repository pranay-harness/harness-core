/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml;

import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.JELENA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.exception.GitConnectionDelegateException;
import io.harness.git.model.GitRepositoryType;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;
import software.wings.beans.GitConfig;
import software.wings.beans.GitOperationContext;

import com.google.inject.Inject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.errors.TransportException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class GitClientHelperTest extends WingsBaseTest {
  @Inject @InjectMocks GitClientHelper gitClientHelper;

  @Test(expected = GitConnectionDelegateException.class)
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_checkIfGitConnectivityIssueInCaseOfTransportException() {
    gitClientHelper.checkIfGitConnectivityIssue(
        new GitAPIException("Git Exception", new TransportException("Transport Exception")) {});
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_checkIfGitConnectivityIssueIsNotTrownInCaseOfOtherExceptions() {
    gitClientHelper.checkIfGitConnectivityIssue(new GitAPIException("newTransportException") {});
  }

  @Test(expected = GitConnectionDelegateException.class)
  @Owner(developers = JELENA)
  @Category(UnitTests.class)
  public void test_checkIfGitConnectivityIssueInCaseOfRefNotFoundException() {
    gitClientHelper.checkIfGitConnectivityIssue(new RefNotFoundException("Invalid commit Id"));
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testGetRepoDirectory() {
    final String repoDirectory =
        gitClientHelper.getRepoDirectory(GitOperationContext.builder()
                                             .gitConnectorId("id")
                                             .gitConfig(GitConfig.builder()
                                                            .accountId("accountId")
                                                            .gitRepoType(GitRepositoryType.HELM)
                                                            .repoUrl("http://github.com/my-repo")
                                                            .build())
                                             .build());
    assertThat(repoDirectory)
        .isEqualTo("./repository/helm/accountId/id/my-repo/9d0502fc8d289f365a3fdcb24607c878b68fad36");
  }
}
