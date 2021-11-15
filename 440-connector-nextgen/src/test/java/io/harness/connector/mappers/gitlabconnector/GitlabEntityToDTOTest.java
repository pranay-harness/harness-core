package io.harness.connector.mappers.gitlabconnector;

import static io.harness.delegate.beans.connector.scm.GitAuthType.HTTP;
import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabHttpAuthentication;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabTokenApiAccess;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccess;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabAuthentication;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnector;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpCredentials;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabKerberos;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabTokenSpec;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabUsernamePassword;
import io.harness.encryption.SecretRefHelper;
import io.harness.rule.Owner;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class GitlabEntityToDTOTest extends CategoryTest {
  @InjectMocks GitlabEntityToDTO gitlabEntityToDTO;

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
    final String privateKeyRef = "privateKeyRef";
    final String validationRepo = "validationRepo";

    final GitlabAuthentication gitlabAuthentication =
        GitlabAuthentication.builder()
            .authType(HTTP)
            .credentials(GitlabHttpCredentials.builder()
                             .type(GitlabHttpAuthenticationType.USERNAME_AND_PASSWORD)
                             .httpCredentialsSpec(GitlabUsernamePassword.builder()
                                                      .passwordRef(SecretRefHelper.createSecretRef(passwordRef))
                                                      .username(username)
                                                      .build())
                             .build())
            .build();

    final GitlabApiAccess gitlabApiAccess =
        GitlabApiAccess.builder()
            .type(GitlabApiAccessType.TOKEN)
            .spec(GitlabTokenSpec.builder().tokenRef(SecretRefHelper.createSecretRef(privateKeyRef)).build())
            .build();
    final GitlabConnector gitlabConnectorDTO = GitlabConnector.builder()
                                                   .url(url)
                                                   .validationRepo(validationRepo)
                                                   .connectionType(GitConnectionType.ACCOUNT)
                                                   .authentication(gitlabAuthentication)
                                                   .apiAccess(gitlabApiAccess)
                                                   .build();

    final io.harness.connector.entities.embedded.gitlabconnector.GitlabConnector gitlabConnector1 =
        io.harness.connector.entities.embedded.gitlabconnector.GitlabConnector.builder()
            .hasApiAccess(true)
            .url(url)
            .validationRepo(validationRepo)
            .gitlabApiAccess(GitlabTokenApiAccess.builder().tokenRef(privateKeyRef).build())
            .connectionType(GitConnectionType.ACCOUNT)
            .authType(HTTP)
            .authenticationDetails(
                GitlabHttpAuthentication.builder()
                    .type(GitlabHttpAuthenticationType.USERNAME_AND_PASSWORD)
                    .auth(io.harness.connector.entities.embedded.gitlabconnector.GitlabUsernamePassword.builder()
                              .username(username)
                              .passwordRef(passwordRef)
                              .build())
                    .build())
            .build();
    final GitlabConnector gitlabConnector = gitlabEntityToDTO.createConnectorDTO(gitlabConnector1);
    ObjectMapper objectMapper = new ObjectMapper();
    assertThat(objectMapper.readTree(objectMapper.writeValueAsString(gitlabConnector)))
        .isEqualTo(objectMapper.readTree(objectMapper.writeValueAsString(gitlabConnectorDTO)));
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity_1() throws IOException {
    final String url = "url";
    final String tokenKeyRef = "privateKeyRef";

    final GitlabAuthentication gitlabAuthentication =
        GitlabAuthentication.builder()
            .authType(HTTP)
            .credentials(
                GitlabHttpCredentials.builder()
                    .type(GitlabHttpAuthenticationType.KERBEROS)
                    .httpCredentialsSpec(
                        GitlabKerberos.builder().kerberosKeyRef(SecretRefHelper.createSecretRef(tokenKeyRef)).build())
                    .build())
            .build();

    final GitlabApiAccess gitlabApiAccess =
        GitlabApiAccess.builder()
            .type(GitlabApiAccessType.TOKEN)
            .spec(GitlabTokenSpec.builder().tokenRef(SecretRefHelper.createSecretRef(tokenKeyRef)).build())
            .build();
    final GitlabConnector gitlabConnectorDTO = GitlabConnector.builder()
                                                   .url(url)
                                                   .connectionType(GitConnectionType.REPO)
                                                   .authentication(gitlabAuthentication)
                                                   .apiAccess(gitlabApiAccess)
                                                   .build();

    final io.harness.connector.entities.embedded.gitlabconnector.GitlabConnector gitlabConnector1 =
        io.harness.connector.entities.embedded.gitlabconnector.GitlabConnector.builder()
            .hasApiAccess(true)
            .url(url)
            .gitlabApiAccess(GitlabTokenApiAccess.builder().tokenRef(tokenKeyRef).build())
            .connectionType(GitConnectionType.REPO)
            .authType(HTTP)
            .authenticationDetails(
                GitlabHttpAuthentication.builder()
                    .type(GitlabHttpAuthenticationType.KERBEROS)
                    .auth(io.harness.connector.entities.embedded.gitlabconnector.GitlabKerberos.builder()
                              .kerberosKeyRef(tokenKeyRef)
                              .build())
                    .build())
            .build();
    final GitlabConnector gitlabConnector = gitlabEntityToDTO.createConnectorDTO(gitlabConnector1);
    ObjectMapper objectMapper = new ObjectMapper();
    assertThat(objectMapper.readTree(objectMapper.writeValueAsString(gitlabConnector)))
        .isEqualTo(objectMapper.readTree(objectMapper.writeValueAsString(gitlabConnectorDTO)));
  }
}
