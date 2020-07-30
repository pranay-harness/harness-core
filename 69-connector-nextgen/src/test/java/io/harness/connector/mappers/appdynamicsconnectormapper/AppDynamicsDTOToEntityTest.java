package io.harness.connector.mappers.appdynamicsconnectormapper;

import static io.harness.rule.OwnerRule.NEMANJA;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.appdynamicsconnector.AppDynamicsConfig;
import io.harness.connector.mappers.appdynamicsmapper.AppDynamicsDTOToEntity;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConfigDTO;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class AppDynamicsDTOToEntityTest extends CategoryTest {
  @InjectMocks AppDynamicsDTOToEntity appDynamicsDTOToEntity;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testToAppDynamicsConfig() {
    String username = "username";
    String encryptedPassword = "encryptedPassword";
    String accountname = "accountname";
    String controllerUrl = "controllerUrl";
    String accountId = "accountId";

    AppDynamicsConfigDTO appDynamicsConfigDTO = AppDynamicsConfigDTO.builder()
                                                    .username(username)
                                                    .passwordReference(encryptedPassword)
                                                    .accountname(accountname)
                                                    .controllerUrl(controllerUrl)
                                                    .accountId(accountId)
                                                    .build();

    AppDynamicsConfig appDynamicsConfig = appDynamicsDTOToEntity.toConnectorEntity(appDynamicsConfigDTO);
    assertThat(appDynamicsConfig).isNotNull();
    assertThat(appDynamicsConfig.getUsername()).isEqualTo(appDynamicsConfigDTO.getUsername());
    assertThat(appDynamicsConfig.getPasswordReference()).isEqualTo(appDynamicsConfigDTO.getPasswordReference());
    assertThat(appDynamicsConfig.getAccountname()).isEqualTo(appDynamicsConfigDTO.getAccountname());
    assertThat(appDynamicsConfig.getControllerUrl()).isEqualTo(appDynamicsConfigDTO.getControllerUrl());
    assertThat(appDynamicsConfig.getAccountId()).isEqualTo(appDynamicsConfigDTO.getAccountId());
  }
}
