package io.harness.connector.mappers.nexusmapper;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.nexusconnector.NexusConnector;
import io.harness.connector.entities.embedded.nexusconnector.NexusUserNamePasswordAuthentication;
import io.harness.connector.mappers.SecretRefHelper;
import io.harness.delegate.beans.connector.nexusconnector.NexusAuthType;
import io.harness.delegate.beans.connector.nexusconnector.NexusAuthenticationDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusUsernamePasswordAuthDTO;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import static io.harness.encryption.Scope.ACCOUNT;
import static org.assertj.core.api.Assertions.assertThat;

public class NexusEntityToDTOTest extends CategoryTest {
  @InjectMocks NexusEntityToDTO nexusEntityToDTO;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.ABHINAV)
  @Category(UnitTests.class)
  public void createConnectorDTOTest() {
    String nexusUrl = "url";
    String userName = "userName";
    String passwordRef = ACCOUNT.getYamlRepresentation() + ".passwordRef";
    final String version = "1.2";

    NexusConnector nexusConnector =
        NexusConnector.builder()
            .authType(NexusAuthType.USER_PASSWORD)
            .url(nexusUrl)
            .nexusVersion(version)
            .nexusAuthentication(
                NexusUserNamePasswordAuthentication.builder().username(userName).passwordRef(passwordRef).build())
            .build();
    NexusConnectorDTO nexusConnectorDTO = nexusEntityToDTO.createConnectorDTO(nexusConnector);
    assertThat(nexusConnectorDTO).isNotNull();
    assertThat(nexusConnectorDTO.getNexusServerUrl()).isEqualTo(nexusUrl);
    assertThat(nexusConnectorDTO.getVersion()).isEqualTo(version);
    NexusAuthenticationDTO nexusAuthenticationDTO = nexusConnectorDTO.getAuth();
    assertThat(nexusAuthenticationDTO).isNotNull();
    assertThat(nexusAuthenticationDTO.getAuthType()).isEqualTo(NexusAuthType.USER_PASSWORD);
    NexusUsernamePasswordAuthDTO nexusUsernamePasswordAuthDTO =
        (NexusUsernamePasswordAuthDTO) nexusAuthenticationDTO.getCredentials();
    assertThat(nexusUsernamePasswordAuthDTO).isNotNull();
    assertThat(nexusUsernamePasswordAuthDTO.getUsername()).isEqualTo(userName);
    assertThat(nexusUsernamePasswordAuthDTO.getPasswordRef()).isEqualTo(SecretRefHelper.createSecretRef(passwordRef));
  }
}