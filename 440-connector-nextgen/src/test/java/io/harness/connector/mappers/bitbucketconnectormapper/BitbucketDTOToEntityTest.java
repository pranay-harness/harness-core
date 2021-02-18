package io.harness.connector.mappers.bitbucketconnectormapper;

import static io.harness.delegate.beans.connector.scm.GitAuthType.HTTP;
import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.powermock.api.mockito.PowerMockito.when;

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
import io.harness.ng.service.SecretRefService;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BitbucketDTOToEntityTest extends CategoryTest {
  @InjectMocks BitbucketDTOToEntity bitbucketDTOToEntity;
  @Mock SecretRefService secretRefService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity_0() {
    final String url = "url";
    final String passwordRefString = "passwordRef";
    final String username = "username";
    final BaseNGAccess ngAccess = BaseNGAccess.builder().accountIdentifier("accountIdentifier").build();
    SecretRefData passwordSecretRef =
        SecretRefData.builder().scope(Scope.ACCOUNT).identifier(passwordRefString).build();
    when(secretRefService.validateAndGetSecretConfigString(passwordSecretRef, ngAccess))
        .thenReturn(passwordSecretRef.toSecretRefStringValue());
    final BitbucketAuthenticationDTO bitbucketAuthenticationDTO =
        BitbucketAuthenticationDTO.builder()
            .authType(HTTP)
            .credentials(BitbucketHttpCredentialsDTO.builder()
                             .type(BitbucketHttpAuthenticationType.USERNAME_AND_PASSWORD)
                             .httpCredentialsSpec(BitbucketUsernamePasswordDTO.builder()
                                                      .passwordRef(passwordSecretRef)
                                                      .username(username)
                                                      .build())
                             .build())
            .build();

    final BitbucketConnectorDTO bitbucketConnectorDTO = BitbucketConnectorDTO.builder()
                                                            .url(url)
                                                            .connectionType(GitConnectionType.REPO)
                                                            .authentication(bitbucketAuthenticationDTO)
                                                            .build();
    final BitbucketConnector bitbucketConnector =
        bitbucketDTOToEntity.toConnectorEntity(bitbucketConnectorDTO, ngAccess);
    assertThat(bitbucketConnector).isNotNull();
    assertThat(bitbucketConnector.getUrl()).isEqualTo(url);
    assertThat(bitbucketConnector.getAuthType()).isEqualTo(HTTP);
    assertThat(bitbucketConnector.getAuthenticationDetails())
        .isEqualTo(BitbucketHttpAuthentication.builder()
                       .type(BitbucketHttpAuthenticationType.USERNAME_AND_PASSWORD)
                       .auth(BitbucketUsernamePassword.builder()
                                 .username(username)
                                 .passwordRef(passwordSecretRef.toSecretRefStringValue())
                                 .build())
                       .build());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity_1() {
    final String url = "url";
    final String passwordRef = "passwordRef";
    final String usernameIdentifier = "usernameRef";
    final BaseNGAccess ngAccess = BaseNGAccess.builder().accountIdentifier("accountIdentifier").build();
    SecretRefData passwordSecretRef = SecretRefData.builder().scope(Scope.ACCOUNT).identifier(passwordRef).build();
    when(secretRefService.validateAndGetSecretConfigString(passwordSecretRef, ngAccess))
        .thenReturn(passwordSecretRef.toSecretRefStringValue());
    SecretRefData usernameRef = SecretRefData.builder().scope(Scope.ACCOUNT).identifier(usernameIdentifier).build();
    when(secretRefService.validateAndGetSecretConfigString(usernameRef, ngAccess))
        .thenReturn(usernameRef.toSecretRefStringValue());

    final BitbucketAuthenticationDTO bitbucketAuthenticationDTO =
        BitbucketAuthenticationDTO.builder()
            .authType(HTTP)
            .credentials(BitbucketHttpCredentialsDTO.builder()
                             .type(BitbucketHttpAuthenticationType.USERNAME_AND_PASSWORD)
                             .httpCredentialsSpec(BitbucketUsernamePasswordDTO.builder()
                                                      .passwordRef(passwordSecretRef)
                                                      .usernameRef(usernameRef)
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
                       .auth(BitbucketUsernamePassword.builder()
                                 .usernameRef(usernameRef.toSecretRefStringValue())
                                 .passwordRef(passwordSecretRef.toSecretRefStringValue())
                                 .build())
                       .build());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity_3() {
    final String url = "url";
    final String sshKeyIdentifier = "sshKeyRef";
    final BaseNGAccess ngAccess = BaseNGAccess.builder().accountIdentifier("accountIdentifier").build();
    SecretRefData sshKeyRef = SecretRefData.builder().scope(Scope.ACCOUNT).identifier(sshKeyIdentifier).build();
    when(secretRefService.validateAndGetSecretConfigString(sshKeyRef, ngAccess))
        .thenReturn(sshKeyRef.toSecretRefStringValue());
    final BitbucketAuthenticationDTO bitbucketAuthenticationDTO =
        BitbucketAuthenticationDTO.builder()
            .authType(GitAuthType.SSH)
            .credentials(BitbucketSshCredentialsDTO.builder().sshKeyRef(sshKeyRef).build())
            .build();

    final BitbucketConnectorDTO bitbucketConnectorDTO = BitbucketConnectorDTO.builder()
                                                            .url(url)
                                                            .connectionType(GitConnectionType.REPO)
                                                            .authentication(bitbucketAuthenticationDTO)
                                                            .build();
    final BitbucketConnector bitbucketConnector =
        bitbucketDTOToEntity.toConnectorEntity(bitbucketConnectorDTO, ngAccess);
    assertThat(bitbucketConnector).isNotNull();
    assertThat(bitbucketConnector.getUrl()).isEqualTo(url);
    assertThat(bitbucketConnector.getAuthType()).isEqualTo(GitAuthType.SSH);
    assertThat(bitbucketConnector.getAuthenticationDetails())
        .isEqualTo(BitbucketSshAuthentication.builder().sshKeyRef(sshKeyRef.toSecretRefStringValue()).build());
    assertThat(bitbucketConnector.getBitbucketApiAccess()).isNull();
  }
}