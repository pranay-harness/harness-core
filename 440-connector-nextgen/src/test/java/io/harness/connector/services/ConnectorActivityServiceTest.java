package io.harness.connector.services;

import static io.harness.EntityType.CONNECTORS;
import static io.harness.connector.impl.ConnectorActivityServiceImpl.CREATION_DESCRIPTION;
import static io.harness.connector.impl.ConnectorActivityServiceImpl.UPDATE_DESCRIPTION;
import static io.harness.ng.core.activityhistory.NGActivityStatus.SUCCESS;
import static io.harness.ng.core.activityhistory.NGActivityType.ENTITY_CREATION;
import static io.harness.ng.core.activityhistory.NGActivityType.ENTITY_UPDATE;
import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.connector.impl.ConnectorActivityServiceImpl;
import io.harness.ng.core.activityhistory.NGActivityType;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import io.harness.ng.core.activityhistory.service.NGActivityService;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

public class ConnectorActivityServiceTest extends CategoryTest {
  @InjectMocks ConnectorActivityServiceImpl connectorActivityService;
  @Mock NGActivityService ngActivityService;
  String connectorName = "connector";
  String accountIdentifier = "accountIdentifier";
  String orgIdentifier = "orgIdentifier";
  String projIdentifier = "projIdentifier";
  String identifier = "identifier";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  private ConnectorInfoDTO createConnector() {
    return ConnectorInfoDTO.builder()
        .name(connectorName)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projIdentifier)
        .identifier(identifier)
        .build();
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void createTestForConnectorCreated() {
    ConnectorInfoDTO connector = createConnector();
    connectorActivityService.create(accountIdentifier, connector, ENTITY_CREATION);
    ArgumentCaptor<NGActivityDTO> argumentCaptor = ArgumentCaptor.forClass(NGActivityDTO.class);
    verify(ngActivityService, times(1)).save(argumentCaptor.capture());
    NGActivityDTO ngActivity = argumentCaptor.getValue();
    verityTheFieldsOFActivity(ngActivity, CREATION_DESCRIPTION, ENTITY_CREATION);
  }

  private void verityTheFieldsOFActivity(NGActivityDTO ngActivity, String description, NGActivityType activityType) {
    assertThat(ngActivity).isNotNull();
    assertThat(ngActivity.getAccountIdentifier()).isEqualTo(accountIdentifier);
    assertThat(ngActivity.getActivityTime()).isGreaterThan(0L);
    assertThat(ngActivity.getDescription()).isEqualTo(description);
    assertThat(ngActivity.getActivityStatus()).isEqualTo(SUCCESS);
    assertThat(ngActivity.getType()).isEqualTo(activityType);

    assertThat(ngActivity.getReferredEntity().getName()).isEqualTo(connectorName);
    assertThat(ngActivity.getReferredEntity().getType()).isEqualTo(CONNECTORS);
    assertThat(ngActivity.getReferredEntity().getEntityRef().getAccountIdentifier()).isEqualTo(accountIdentifier);
    assertThat(ngActivity.getReferredEntity().getEntityRef().getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(ngActivity.getReferredEntity().getEntityRef().getProjectIdentifier()).isEqualTo(projIdentifier);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void createTestForConnectorUpdate() {
    ConnectorInfoDTO connector = createConnector();
    connectorActivityService.create(accountIdentifier, connector, ENTITY_UPDATE);
    ArgumentCaptor<NGActivityDTO> argumentCaptor = ArgumentCaptor.forClass(NGActivityDTO.class);
    verify(ngActivityService, times(1)).save(argumentCaptor.capture());
    NGActivityDTO ngActivity = argumentCaptor.getValue();
    verityTheFieldsOFActivity(ngActivity, UPDATE_DESCRIPTION, ENTITY_UPDATE);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void deleteAllActivities() {
    String connectorFQN = "connectorFQN";
    connectorActivityService.deleteAllActivities(accountIdentifier, connectorFQN);
    ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(ngActivityService, times(1))
        .deleteAllActivitiesOfAnEntity(stringArgumentCaptor.capture(), stringArgumentCaptor.capture());
    List<String> arguments = stringArgumentCaptor.getAllValues();
    assertThat(arguments.get(0)).isEqualTo(accountIdentifier);
    assertThat(arguments.get(1)).isEqualTo(connectorFQN);
  }
}
