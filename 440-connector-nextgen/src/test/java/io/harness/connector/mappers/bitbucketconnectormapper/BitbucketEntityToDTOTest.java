package io.harness.connector.mappers.bitbucketconnectormapper;

import static io.harness.delegate.beans.connector.scm.GitAuthType.HTTP;
import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketHttpAuthentication;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketUsernamePasswordApiAccess;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccess;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccessType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketAuthentication;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnector;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpCredentials;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernamePassword;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernameTokenApiAccess;
import io.harness.encryption.SecretRefHelper;
import io.harness.rule.Owner;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class BitbucketEntityToDTOTest extends CategoryTest {
  @InjectMocks BitbucketEntityToDTO bitbucketEntityToDTO;

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

    final BitbucketAuthentication bitbucketAuthentication =
        BitbucketAuthentication.builder()
            .authType(HTTP)
            .credentials(BitbucketHttpCredentials.builder()
                             .type(BitbucketHttpAuthenticationType.USERNAME_AND_PASSWORD)
                             .httpCredentialsSpec(BitbucketUsernamePassword.builder()
                                                      .passwordRef(SecretRefHelper.createSecretRef(passwordRef))
                                                      .username(username)
                                                      .build())
                             .build())
            .build();

    final BitbucketApiAccess bitbucketApiAccess =
        BitbucketApiAccess.builder()
            .type(BitbucketApiAccessType.USERNAME_AND_TOKEN)
            .spec(BitbucketUsernameTokenApiAccess.builder()
                      .usernameRef(SecretRefHelper.createSecretRef(privateKeyRef))
                      .tokenRef(SecretRefHelper.createSecretRef(privateKeyRef))
                      .build())
            .build();
    final BitbucketConnector bitbucketConnectorDTO = BitbucketConnector.builder()
                                                         .url(url)
                                                         .validationRepo(validationRepo)
                                                         .connectionType(GitConnectionType.ACCOUNT)
                                                         .authentication(bitbucketAuthentication)
                                                         .apiAccess(bitbucketApiAccess)
                                                         .build();

    final io.harness.connector.entities.embedded.bitbucketconnector.BitbucketConnector bitbucketConnector1 =
        io.harness.connector.entities.embedded.bitbucketconnector.BitbucketConnector.builder()
            .hasApiAccess(true)
            .url(url)
            .validationRepo(validationRepo)
            .bitbucketApiAccess(
                BitbucketUsernamePasswordApiAccess.builder().usernameRef(privateKeyRef).tokenRef(privateKeyRef).build())
            .connectionType(GitConnectionType.ACCOUNT)
            .authType(HTTP)
            .authenticationDetails(
                BitbucketHttpAuthentication.builder()
                    .type(BitbucketHttpAuthenticationType.USERNAME_AND_PASSWORD)
                    .auth(io.harness.connector.entities.embedded.bitbucketconnector.BitbucketUsernamePassword.builder()
                              .username(username)
                              .passwordRef(passwordRef)
                              .build())
                    .build())
            .build();
    final BitbucketConnector bitbucketConnector = bitbucketEntityToDTO.createConnectorDTO(bitbucketConnector1);
    ObjectMapper objectMapper = new ObjectMapper();
    assertThat(objectMapper.readTree(objectMapper.writeValueAsString(bitbucketConnector)))
        .isEqualTo(objectMapper.readTree(objectMapper.writeValueAsString(bitbucketConnectorDTO)));
  }
}
