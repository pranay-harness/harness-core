package io.harness.delegate.git;

import static io.harness.rule.OwnerRule.ABHINAV;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.MockableTestMixin;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.gitconnector.GitAuthType;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitHTTPAuthenticationDTO;
import io.harness.encryption.SecretRefData;
import io.harness.git.GitClientV2;
import io.harness.git.model.GitBaseRequest;
import io.harness.git.model.GitRepositoryType;
import io.harness.rule.Owner;
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
    GitConfigDTO gitConfigDTO = GitConfigDTO.builder()
                                    .gitAuth(GitHTTPAuthenticationDTO.builder()
                                                 .username("username")
                                                 .passwordRef(SecretRefData.builder().build())
                                                 .build())
                                    .gitAuthType(GitAuthType.HTTP)
                                    .accountId(accountId)
                                    .build();
    final GitBaseRequest gitBaseRequest = GitBaseRequest.builder().build();
    ngGitService.setGitBaseRequest(gitConfigDTO, accountId, gitBaseRequest, GitRepositoryType.YAML);
    assertThat(gitBaseRequest).isNotNull();
    assertThat(gitBaseRequest.getRepoType()).isNotNull();
    assertThat(gitBaseRequest.getAccountId()).isNotNull();
    assertThat(gitBaseRequest.getAuthRequest()).isNotNull();
  }
}