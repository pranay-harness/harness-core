package io.harness.connector.mappers.appdynamicsconnectormapper;

import static io.harness.rule.OwnerRule.NEMANJA;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.appdynamicsconnector.AppDynamicsConfig;
import io.harness.connector.mappers.appdynamicsmapper.AppDynamicsEntityToDTO;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConfigDTO;
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
  public void testCreateAppDynamicsConfigDTO() {
    String username = "username";
    String encryptedPassword = "encryptedPassword";
    String accountname = "accountname";
    String controllerUrl = "controllerUrl";
    String accountId = "accountId";

    AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                              .username(username)
                                              .passwordReference(encryptedPassword)
                                              .accountname(accountname)
                                              .controllerUrl(controllerUrl)
                                              .accountId(accountId)
                                              .build();

    AppDynamicsConfigDTO appDynamicsConfigDTO = appDynamicsEntityToDTO.createConnectorDTO(appDynamicsConfig);
    assertThat(appDynamicsConfigDTO).isNotNull();
    assertThat(appDynamicsConfigDTO.getUsername()).isEqualTo(appDynamicsConfig.getUsername());
    assertThat(appDynamicsConfigDTO.getPasswordReference()).isEqualTo(appDynamicsConfig.getPasswordReference());
    assertThat(appDynamicsConfigDTO.getAccountname()).isEqualTo(appDynamicsConfig.getAccountname());
    assertThat(appDynamicsConfigDTO.getControllerUrl()).isEqualTo(appDynamicsConfig.getControllerUrl());
    assertThat(appDynamicsConfigDTO.getAccountId()).isEqualTo(appDynamicsConfig.getAccountId());
  }
}
