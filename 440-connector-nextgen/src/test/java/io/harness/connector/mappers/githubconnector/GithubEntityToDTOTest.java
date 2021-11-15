package io.harness.connector.mappers.githubconnector;

import static io.harness.delegate.beans.connector.scm.GitAuthType.HTTP;
import static io.harness.delegate.beans.connector.scm.github.GithubApiAccessType.GITHUB_APP;
import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.githubconnector.GithubAppApiAccess;
import io.harness.connector.entities.embedded.githubconnector.GithubHttpAuthentication;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccess;
import io.harness.delegate.beans.connector.scm.github.GithubAppSpec;
import io.harness.delegate.beans.connector.scm.github.GithubAuthentication;
import io.harness.delegate.beans.connector.scm.github.GithubConnector;
import io.harness.delegate.beans.connector.scm.github.GithubHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentials;
import io.harness.delegate.beans.connector.scm.github.GithubUsernamePassword;
import io.harness.encryption.SecretRefHelper;
import io.harness.rule.Owner;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class GithubEntityToDTOTest extends CategoryTest {
  @InjectMocks GithubEntityToDTO githubEntityToDTO;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity_0() throws IOException {
    final String url = "url";
    final String passwordRef = "passwordRef";
    final String username = "username";
    final String appId = "appId";
    final String insId = "insId";
    final String tokenRef = "tokenRef";
    final String privateKeyRef = "privateKeyRef";
    final String validationRepo = "validationRepo";

    final GithubAuthentication githubAuthentication =
        GithubAuthentication.builder()
            .authType(HTTP)
            .credentials(GithubHttpCredentials.builder()
                             .type(GithubHttpAuthenticationType.USERNAME_AND_PASSWORD)
                             .httpCredentialsSpec(GithubUsernamePassword.builder()
                                                      .passwordRef(SecretRefHelper.createSecretRef(passwordRef))
                                                      .username(username)
                                                      .build())
                             .build())
            .build();

    final GithubApiAccess githubApiAccess = GithubApiAccess.builder()
                                                .type(GITHUB_APP)
                                                .spec(GithubAppSpec.builder()
                                                          .applicationId(appId)
                                                          .installationId(insId)
                                                          .privateKeyRef(SecretRefHelper.createSecretRef(privateKeyRef))
                                                          .build())
                                                .build();
    final GithubConnector githubConnectorDTO = GithubConnector.builder()
                                                   .url(url)
                                                   .validationRepo(validationRepo)
                                                   .connectionType(GitConnectionType.ACCOUNT)
                                                   .authentication(githubAuthentication)
                                                   .apiAccess(githubApiAccess)
                                                   .build();

    final io.harness.connector.entities.embedded.githubconnector.GithubConnector githubConnector1 =
        io.harness.connector.entities.embedded.githubconnector.GithubConnector.builder()
            .hasApiAccess(true)
            .url(url)
            .validationRepo(validationRepo)
            .githubApiAccess(GithubAppApiAccess.builder()
                                 .applicationId(appId)
                                 .installationId(insId)
                                 .privateKeyRef(privateKeyRef)
                                 .build())
            .apiAccessType(GITHUB_APP)
            .connectionType(GitConnectionType.ACCOUNT)
            .authType(HTTP)
            .authenticationDetails(
                GithubHttpAuthentication.builder()
                    .type(GithubHttpAuthenticationType.USERNAME_AND_PASSWORD)
                    .auth(io.harness.connector.entities.embedded.githubconnector.GithubUsernamePassword.builder()
                              .username(username)
                              .passwordRef(passwordRef)
                              .build())
                    .build())
            .build();
    final GithubConnector githubConnector = githubEntityToDTO.createConnectorDTO(githubConnector1);
    ObjectMapper objectMapper = new ObjectMapper();
    assertThat(objectMapper.readTree(objectMapper.writeValueAsString(githubConnector)))
        .isEqualTo(objectMapper.readTree(objectMapper.writeValueAsString(githubConnectorDTO)));
  }
}
