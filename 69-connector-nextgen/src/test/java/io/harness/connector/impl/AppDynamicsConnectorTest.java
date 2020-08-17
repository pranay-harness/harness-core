package io.harness.connector.impl;

import static io.harness.delegate.beans.connector.ConnectorType.APP_DYNAMICS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.connector.entities.embedded.appdynamicsconnector.AppDynamicsConnector;
import io.harness.connector.mappers.ConnectorMapper;
import io.harness.connector.repositories.base.ConnectorRepository;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

@Slf4j
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AppDynamicsConnectorTest extends CategoryTest {
  @Mock ConnectorMapper connectorMapper;
  @Mock ConnectorRepository connectorRepository;
  @InjectMocks DefaultConnectorServiceImpl connectorService;

  String userName = "userName";
  String password = "password";
  String identifier = "identifier";
  String name = "name";
  String controllerUrl = "https://xwz.com";
  String accountName = "accountName";
  ConnectorRequestDTO connectorRequestDTO;
  ConnectorDTO connectorDTO;
  AppDynamicsConnector connector;
  String accountIdentifier = "accountIdentifier";
  @Rule public ExpectedException expectedEx = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    connector = AppDynamicsConnector.builder()
                    .username(userName)
                    .accountId(accountIdentifier)
                    .accountname(accountName)
                    .controllerUrl(controllerUrl)
                    .passwordReference(password)
                    .build();
    connector.setType(APP_DYNAMICS);
    connector.setIdentifier(identifier);
    connector.setName(name);

    AppDynamicsConnectorDTO appDynamicsConnectorDTO = AppDynamicsConnectorDTO.builder()
                                                          .username(userName)
                                                          .accountId(accountIdentifier)
                                                          .accountname(accountName)
                                                          .controllerUrl(controllerUrl)
                                                          .passwordReference(password)
                                                          .build();

    connectorRequestDTO = ConnectorRequestDTO.builder()
                              .name(name)
                              .identifier(identifier)
                              .connectorType(APP_DYNAMICS)
                              .connectorConfig(appDynamicsConnectorDTO)
                              .build();

    connectorDTO = ConnectorDTO.builder()
                       .name(name)
                       .identifier(identifier)
                       .connectorType(APP_DYNAMICS)
                       .connectorConfig(appDynamicsConnectorDTO)
                       .build();

    when(connectorRepository.save(connector)).thenReturn(connector);
    when(connectorMapper.writeDTO(connector)).thenReturn(connectorDTO);
    when(connectorMapper.toConnector(connectorRequestDTO, accountIdentifier)).thenReturn(connector);
  }

  private ConnectorDTO createConnector() {
    return connectorService.create(connectorRequestDTO, accountIdentifier);
  }

  @Test
  @Owner(developers = OwnerRule.NEMANJA)
  @Category(UnitTests.class)
  public void testCreateAppDynamicsConnector() {
    ConnectorDTO connectorDTOOutput = createConnector();
    ensureAppDynamicsConnectorFieldsAreCorrect(connectorDTOOutput);
  }

  @Test
  @Owner(developers = OwnerRule.NEMANJA)
  @Category(UnitTests.class)
  public void testGetAppDynamicsConnector() {
    createConnector();
    when(connectorRepository.findByFullyQualifiedIdentifier(anyString())).thenReturn(Optional.of(connector));
    ConnectorDTO connectorDTO = connectorService.get(accountIdentifier, null, null, identifier).get();
    ensureAppDynamicsConnectorFieldsAreCorrect(connectorDTO);
  }

  private void ensureAppDynamicsConnectorFieldsAreCorrect(ConnectorDTO connectorDTOOutput) {
    assertThat(connectorDTOOutput).isNotNull();
    assertThat(connectorDTOOutput.getName()).isEqualTo(name);
    assertThat(connectorDTOOutput.getIdentifier()).isEqualTo(identifier);
    assertThat(connectorDTOOutput.getConnectorType()).isEqualTo(APP_DYNAMICS);
    AppDynamicsConnectorDTO appDynamicsConnectorDTO = (AppDynamicsConnectorDTO) connectorDTOOutput.getConnectorConfig();
    assertThat(appDynamicsConnectorDTO).isNotNull();
    assertThat(appDynamicsConnectorDTO.getUsername()).isEqualTo(userName);
    assertThat(appDynamicsConnectorDTO.getPasswordReference()).isEqualTo(password);
    assertThat(appDynamicsConnectorDTO.getAccountname()).isEqualTo(accountName);
    assertThat(appDynamicsConnectorDTO.getControllerUrl()).isEqualTo(controllerUrl);
    assertThat(appDynamicsConnectorDTO.getAccountId()).isEqualTo(accountIdentifier);
  }
}