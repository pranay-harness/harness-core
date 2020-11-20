package io.harness.connector.mappers.nexusmapper;

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
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;

public class NexusDTOToEntityTest extends CategoryTest {
  @InjectMocks NexusDTOToEntity nexusDTOToEntity;

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

    SecretRefData passwordSecretRef =
        SecretRefData.builder().identifier(passwordRefIdentifier).scope(Scope.ACCOUNT).build();

    NexusUsernamePasswordAuthDTO nexusUsernamePasswordAuthDTO =
        NexusUsernamePasswordAuthDTO.builder().username(userName).passwordRef(passwordSecretRef).build();

    NexusAuthenticationDTO nexusAuthenticationDTO = NexusAuthenticationDTO.builder()
                                                        .authType(NexusAuthType.USER_PASSWORD)
                                                        .credentials(nexusUsernamePasswordAuthDTO)
                                                        .build();
    NexusConnectorDTO nexusConnectorDTO =
        NexusConnectorDTO.builder().nexusServerUrl(url).version(version).auth(nexusAuthenticationDTO).build();
    NexusConnector nexusConnector = nexusDTOToEntity.toConnectorEntity(nexusConnectorDTO);
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
