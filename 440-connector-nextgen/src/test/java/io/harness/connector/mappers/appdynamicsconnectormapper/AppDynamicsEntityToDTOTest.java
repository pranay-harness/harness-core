package io.harness.connector.mappers.appdynamicsconnectormapper;

import static io.harness.rule.OwnerRule.NEMANJA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.appdynamicsconnector.AppDynamicsConnector;
import io.harness.connector.mappers.appdynamicsmapper.AppDynamicsEntityToDTO;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class AppDynamicsEntityToDTOTest extends CategoryTest {
  @InjectMocks AppDynamicsEntityToDTO appDynamicsEntityToDTO;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testCreateAppDynamicsConnectorDTO() {
    String username = "username";
    String passwordRef = "passwordRef";
    String accountname = "accountname";
    String controllerUrl = "controllerUrl";
    String accountId = "accountId";

    AppDynamicsConnector appDynamicsConnector = AppDynamicsConnector.builder()
                                                    .username(username)
                                                    .passwordRef(passwordRef)
                                                    .accountname(accountname)
                                                    .controllerUrl(controllerUrl)
                                                    .accountId(accountId)
                                                    .build();

    AppDynamicsConnectorDTO appDynamicsConnectorDTO = appDynamicsEntityToDTO.createConnectorDTO(appDynamicsConnector);
    assertThat(appDynamicsConnectorDTO).isNotNull();
    assertThat(appDynamicsConnectorDTO.getUsername()).isEqualTo(appDynamicsConnector.getUsername());
    assertThat(appDynamicsConnectorDTO.getPasswordRef()).isNotNull();
    assertThat(appDynamicsConnectorDTO.getPasswordRef().getIdentifier())
        .isEqualTo(appDynamicsConnector.getPasswordRef());
    assertThat(appDynamicsConnectorDTO.getAccountname()).isEqualTo(appDynamicsConnector.getAccountname());
    assertThat(appDynamicsConnectorDTO.getControllerUrl()).isEqualTo(appDynamicsConnector.getControllerUrl() + "/");
    assertThat(appDynamicsConnectorDTO.getAccountId()).isEqualTo(appDynamicsConnector.getAccountId());
  }
}
