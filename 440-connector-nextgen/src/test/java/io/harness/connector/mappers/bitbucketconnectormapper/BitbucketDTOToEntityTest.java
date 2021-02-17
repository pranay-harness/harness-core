package io.harness.connector.mappers.bitbucketconnectormapper;

import static io.harness.delegate.beans.connector.scm.GitAuthType.HTTP;
import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketConnector;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketHttpAuthentication;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketSshAuthentication;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketUsernamePassword;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketSshCredentialsDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernamePasswordDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.ng.core.BaseNGAccess;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class BitbucketDTOToEntityTest extends CategoryTest {
  @InjectMocks BitbucketDTOToEntity bitbucketDTOToEntity;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity_0() {
    final String url = "url";
    final String passwordRef = "passwordRef";
    final String username = "username";
    final String appId = "appId";
    final String insId = "insId";
    final String privateKeyRef = "privateKeyRef";

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

    final BitbucketConnectorDTO bitbucketConnectorDTO = BitbucketConnectorDTO.builder()
                                                            .url(url)
                                                            .connectionType(GitConnectionType.REPO)
                                                            .authentication(bitbucketAuthenticationDTO)
                                                            .build();
    final BitbucketConnector bitbucketConnector = bitbucketDTOToEntity.toConnectorEntity(
        bitbucketConnectorDTO, BaseNGAccess.builder().accountIdentifier("accountIdentifier").build());
    assertThat(bitbucketConnector).isNotNull();
    assertThat(bitbucketConnector.getUrl()).isEqualTo(url);
    assertThat(bitbucketConnector.getAuthType()).isEqualTo(HTTP);
    assertThat(bitbucketConnector.getAuthenticationDetails())
        .isEqualTo(BitbucketHttpAuthentication.builder()
                       .type(BitbucketHttpAuthenticationType.USERNAME_AND_PASSWORD)
                       .auth(BitbucketUsernamePassword.builder().username(username).passwordRef(passwordRef).build())
                       .build());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity_1() {
    final String url = "url";
    final String passwordRef = "passwordRef";
    final String usernameRef = "usernameRef";
    final String appId = "appId";
    final String insId = "insId";
    final String privateKeyRef = "privateKeyRef";

    final BitbucketAuthenticationDTO bitbucketAuthenticationDTO =
        BitbucketAuthenticationDTO.builder()
            .authType(HTTP)
            .credentials(
                BitbucketHttpCredentialsDTO.builder()
                    .type(BitbucketHttpAuthenticationType.USERNAME_AND_PASSWORD)
                    .httpCredentialsSpec(
                        BitbucketUsernamePasswordDTO.builder()
                            .passwordRef(SecretRefData.builder().scope(Scope.ACCOUNT).identifier(passwordRef).build())
                            .usernameRef(SecretRefData.builder().scope(Scope.ACCOUNT).identifier(usernameRef).build())
                            .build())
                    .build())
            .build();

    final BitbucketConnectorDTO bitbucketConnectorDTO = BitbucketConnectorDTO.builder()
                                                            .url(url)
                                                            .connectionType(GitConnectionType.REPO)
                                                            .authentication(bitbucketAuthenticationDTO)
                                                            .build();
    final BitbucketConnector bitbucketConnector = bitbucketDTOToEntity.toConnectorEntity(
        bitbucketConnectorDTO, BaseNGAccess.builder().accountIdentifier("accountIdentifier").build());
    assertThat(bitbucketConnector).isNotNull();
    assertThat(bitbucketConnector.getUrl()).isEqualTo(url);
    assertThat(bitbucketConnector.getAuthType()).isEqualTo(HTTP);
    assertThat(bitbucketConnector.getAuthenticationDetails())
        .isEqualTo(
            BitbucketHttpAuthentication.builder()
                .type(BitbucketHttpAuthenticationType.USERNAME_AND_PASSWORD)
                .auth(BitbucketUsernamePassword.builder().usernameRef(usernameRef).passwordRef(passwordRef).build())
                .build());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity_3() {
    final String url = "url";
    final String sshKeyRef = "sshKeyRef";
    final BitbucketAuthenticationDTO bitbucketAuthenticationDTO =
        BitbucketAuthenticationDTO.builder()
            .authType(GitAuthType.SSH)
            .credentials(BitbucketSshCredentialsDTO.builder()
                             .sshKeyRef(SecretRefData.builder().scope(Scope.ACCOUNT).identifier(sshKeyRef).build())
                             .build())
            .build();

    final BitbucketConnectorDTO bitbucketConnectorDTO = BitbucketConnectorDTO.builder()
                                                            .url(url)
                                                            .connectionType(GitConnectionType.REPO)
                                                            .authentication(bitbucketAuthenticationDTO)
                                                            .build();
    final BitbucketConnector bitbucketConnector = bitbucketDTOToEntity.toConnectorEntity(
        bitbucketConnectorDTO, BaseNGAccess.builder().accountIdentifier("accountIdentifier").build());
    assertThat(bitbucketConnector).isNotNull();
    assertThat(bitbucketConnector.getUrl()).isEqualTo(url);
    assertThat(bitbucketConnector.getAuthType()).isEqualTo(GitAuthType.SSH);
    assertThat(bitbucketConnector.getAuthenticationDetails())
        .isEqualTo(BitbucketSshAuthentication.builder().sshKeyRef(sshKeyRef).build());
    assertThat(bitbucketConnector.getBitbucketApiAccess()).isNull();
  }
}