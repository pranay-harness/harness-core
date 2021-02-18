package io.harness.connector.mappers.bitbucketconnectormapper;

import static io.harness.delegate.beans.connector.scm.GitAuthType.HTTP;
import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketConnector;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketHttpAuthentication;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketUsernamePassword;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketUsernamePasswordApiAccess;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccessDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccessType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernamePasswordDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernameTokenApiAccessDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.ng.impl.SecretRefServiceImpl;
import io.harness.ng.service.SecretRefService;
import io.harness.rule.Owner;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BitbucketEntityToDTOTest {
  @InjectMocks BitbucketEntityToDTO bitbucketEntityToDTO;
  @Mock SecretRefService secretRefService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(secretRefService.createSecretRef(any())).thenCallRealMethod();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity_0() throws IOException {
    final String url = "url";
    final String passwordRef = "passwordRef";
    final String username = "username";
    final String privateKeyRef = "privateKeyRef";
    final String usernameRef = "usernameRef";

    final BitbucketAuthenticationDTO bitbucketAuthenticationDTO =
        BitbucketAuthenticationDTO.builder()
            .authType(HTTP)
            .credentials(
                BitbucketHttpCredentialsDTO.builder()
                    .type(BitbucketHttpAuthenticationType.USERNAME_AND_PASSWORD)
                    .httpCredentialsSpec(
                        BitbucketUsernamePasswordDTO.builder()
                            .passwordRef(SecretRefData.builder().scope(Scope.ACCOUNT).identifier(passwordRef).build())
                            .username(username)
                            .build())
                    .build())
            .build();

    final BitbucketApiAccessDTO bitbucketApiAccessDTO =
        BitbucketApiAccessDTO.builder()
            .type(BitbucketApiAccessType.USERNAME_AND_TOKEN)
            .spec(BitbucketUsernameTokenApiAccessDTO.builder()
                      .usernameRef(SecretRefData.builder().scope(Scope.ACCOUNT).identifier(usernameRef).build())
                      .tokenRef(SecretRefData.builder().scope(Scope.ACCOUNT).identifier(privateKeyRef).build())
                      .build())
            .build();
    final BitbucketConnectorDTO bitbucketConnectorDTO = BitbucketConnectorDTO.builder()
                                                            .url(url)
                                                            .connectionType(GitConnectionType.REPO)
                                                            .authentication(bitbucketAuthenticationDTO)
                                                            .apiAccess(bitbucketApiAccessDTO)
                                                            .build();

    final BitbucketConnector bitbucketConnector1 =
        BitbucketConnector.builder()
            .hasApiAccess(true)
            .url(url)
            .bitbucketApiAccess(
                BitbucketUsernamePasswordApiAccess.builder().usernameRef(privateKeyRef).tokenRef(privateKeyRef).build())
            .connectionType(GitConnectionType.REPO)
            .authType(HTTP)
            .authenticationDetails(
                BitbucketHttpAuthentication.builder()
                    .type(BitbucketHttpAuthenticationType.USERNAME_AND_PASSWORD)
                    .auth(BitbucketUsernamePassword.builder().username(username).passwordRef(passwordRef).build())
                    .build())
            .build();
    final BitbucketConnectorDTO bitbucketConnector = bitbucketEntityToDTO.createConnectorDTO(bitbucketConnector1);
    ObjectMapper objectMapper = new ObjectMapper();
    assertThat(objectMapper.readTree(objectMapper.writeValueAsString(bitbucketConnector)))
        .isEqualTo(objectMapper.readTree(objectMapper.writeValueAsString(bitbucketConnectorDTO)));
  }
}
