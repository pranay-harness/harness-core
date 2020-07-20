package io.harness.cdng.gitclient;

import static io.harness.rule.OwnerRule.ABHINAV;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.gitconnector.GitAuthType;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConnectionType;
import io.harness.delegate.beans.connector.gitconnector.GitHTTPAuthenticationDTO;
import io.harness.rule.Owner;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.WingsBaseTest;

public class GitClientNGTest extends WingsBaseTest {
  public static final String ACCOUNT_ID = "ACCOUNT_ID";
  @Inject @Spy GitClientNGImpl gitClientNG;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testValidate() throws GitAPIException {
    GitConfigDTO gitConfig = GitConfigDTO.builder()
                                 .gitAuth(GitHTTPAuthenticationDTO.builder()
                                              .gitConnectionType(GitConnectionType.REPO)
                                              .accountId(ACCOUNT_ID)
                                              .branchName("branchName")
                                              .encryptedPassword("abcd")
                                              .url("http://url.com")
                                              .username("username")
                                              .build())
                                 .gitAuthType(GitAuthType.HTTP)
                                 .build();
    doThrow(new JGitInternalException("Exception caught during execution of ls-remote command"))
        .when(gitClientNG)
        .initGitAndGetBranches(any());
    gitClientNG.validate(gitConfig);
  }
}
