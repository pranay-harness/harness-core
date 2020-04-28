package software.wings.service.impl.yaml;

import static io.harness.rule.OwnerRule.DEEPAK;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.TransportException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;

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
}