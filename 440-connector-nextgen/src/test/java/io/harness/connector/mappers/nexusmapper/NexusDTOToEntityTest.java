package io.harness.connector.mappers.nexusmapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.nexusconnector.NexusConnector;
import io.harness.connector.entities.embedded.nexusconnector.NexusUserNamePasswordAuthentication;
import io.harness.delegate.beans.connector.nexusconnector.NexusAuthType;
import io.harness.delegate.beans.connector.nexusconnector.NexusAuthenticationDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusUsernamePasswordAuthDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.service.SecretRefService;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class NexusDTOToEntityTest extends CategoryTest {
  @InjectMocks NexusDTOToEntity nexusDTOToEntity;
  @Mock SecretRefService secretRefService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.ABHINAV)
  @Category(UnitTests.class)
  public void toConnectorEntityTest() {
    String url = "url";
    String userName = "userName";
    String passwordRefIdentifier = "passwordRefIdentifier";
    final String version = "1.2";

    final BaseNGAccess ngAccess = BaseNGAccess.builder().accountIdentifier("accountIdentifier").build();
    SecretRefData passwordSecretRef =
        SecretRefData.builder().scope(Scope.ACCOUNT).identifier(passwordRefIdentifier).build();
    when(secretRefService.validateAndGetSecretConfigString(passwordSecretRef, ngAccess))
        .thenReturn(passwordSecretRef.toSecretRefStringValue());

    NexusUsernamePasswordAuthDTO nexusUsernamePasswordAuthDTO =
        NexusUsernamePasswordAuthDTO.builder().username(userName).passwordRef(passwordSecretRef).build();

    NexusAuthenticationDTO nexusAuthenticationDTO = NexusAuthenticationDTO.builder()
                                                        .authType(NexusAuthType.USER_PASSWORD)
                                                        .credentials(nexusUsernamePasswordAuthDTO)
                                                        .build();
    NexusConnectorDTO nexusConnectorDTO =
        NexusConnectorDTO.builder().nexusServerUrl(url).version(version).auth(nexusAuthenticationDTO).build();
    NexusConnector nexusConnector = nexusDTOToEntity.toConnectorEntity(
        nexusConnectorDTO, BaseNGAccess.builder().accountIdentifier("accountIdentifier").build());
    assertThat(nexusConnector).isNotNull();
    assertThat(nexusConnector.getUrl()).isEqualTo(url);
    assertThat(nexusConnector.getNexusVersion()).isEqualTo(version);
    assertThat(((NexusUserNamePasswordAuthentication) (nexusConnector.getNexusAuthentication())).getUsername())
        .isEqualTo(userName);
    assertThat(((NexusUserNamePasswordAuthentication) (nexusConnector.getNexusAuthentication())).getPasswordRef())
        .isEqualTo(passwordSecretRef.toSecretRefStringValue());
    assertThat(nexusConnector.getAuthType()).isEqualTo(NexusAuthType.USER_PASSWORD);
  }
}
