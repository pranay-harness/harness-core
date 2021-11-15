package io.harness.connector.service.git;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.MockableTestMixin;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfig;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthentication;
import io.harness.encryption.SecretRefData;
import io.harness.git.GitClientV2;
import io.harness.git.model.GitBaseRequest;
import io.harness.git.model.GitRepositoryType;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class NGGitServiceImplTest extends CategoryTest implements MockableTestMixin {
  @Mock GitClientV2 gitClientV2;
  @InjectMocks @Inject NGGitServiceImpl ngGitService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetGitBaseRequest() {
    final String accountId = "accountId";
    GitConfig gitConfig = GitConfig.builder()
                              .gitAuth(GitHTTPAuthentication.builder()
                                           .username("username")
                                           .passwordRef(SecretRefData.builder().build())
                                           .build())
                              .gitAuthType(GitAuthType.HTTP)
                              .build();
    final GitBaseRequest gitBaseRequest = GitBaseRequest.builder().build();
    ngGitService.setGitBaseRequest(gitConfig, accountId, gitBaseRequest, GitRepositoryType.YAML, null);
    assertThat(gitBaseRequest).isNotNull();
    assertThat(gitBaseRequest.getRepoType()).isNotNull();
    assertThat(gitBaseRequest.getAccountId()).isNotNull();
    assertThat(gitBaseRequest.getAuthRequest()).isNotNull();
  }
}
