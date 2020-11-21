package io.harness.connector.impl;

import io.harness.EntityType;
import io.harness.beans.IdentifierRef;
import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.connector.services.ConnectorActivityService;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.activityhistory.NGActivityStatus;
import io.harness.ng.core.activityhistory.NGActivityType;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import io.harness.ng.core.activityhistory.service.NGActivityService;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class ConnectorActivityServiceImpl implements ConnectorActivityService {
  public final static String CREATION_DESCRIPTION = "Connector Created";
  public final static String UPDATE_DESCRIPTION = "Connector Updated";
  private NGActivityService ngActivityService;

  @Override
  public void create(String accountIdentifier, ConnectorInfoDTO connector, NGActivityType ngActivityType) {
    if (ngActivityType == NGActivityType.ENTITY_CREATION) {
      createConnectorCreationActivity(accountIdentifier, connector);
    } else if (ngActivityType == NGActivityType.ENTITY_UPDATE) {
      createConnectorUpdateActivity(accountIdentifier, connector);
    }
  }

  private EntityDetail getConnectorEntityDetail(String accountIdentifier, ConnectorInfoDTO connector) {
    IdentifierRef entityRef = IdentifierRefHelper.getIdentifierRefFromEntityIdentifiers(
        connector.getIdentifier(), accountIdentifier, connector.getOrgIdentifier(), connector.getProjectIdentifier());
    return EntityDetail.builder().type(EntityType.CONNECTORS).name(connector.getName()).entityRef(entityRef).build();
  }

  private NGActivityDTO createNGActivityObject(
      String accountIdentifier, ConnectorInfoDTO connector, String activityDescription, NGActivityType type) {
    EntityDetail referredEntity = getConnectorEntityDetail(accountIdentifier, connector);
    return NGActivityDTO.builder()
        .accountIdentifier(accountIdentifier)
        .activityStatus(NGActivityStatus.SUCCESS)
        .description(activityDescription)
        .referredEntity(referredEntity)
        .type(type)
        .activityTime(System.currentTimeMillis())
        .build();
  }

  private void createConnectorCreationActivity(String accountIdentifier, ConnectorInfoDTO connector) {
    NGActivityDTO creationActivity =
        createNGActivityObject(accountIdentifier, connector, CREATION_DESCRIPTION, NGActivityType.ENTITY_CREATION);
    ngActivityService.save(creationActivity);
  }

  private void createConnectorUpdateActivity(String accountIdentifier, ConnectorInfoDTO connector) {
    NGActivityDTO creationActivity =
        createNGActivityObject(accountIdentifier, connector, UPDATE_DESCRIPTION, NGActivityType.ENTITY_UPDATE);
    ngActivityService.save(creationActivity);
  }

  @Override
  public void deleteAllActivities(String accountIdentifier, String connectorFQN) {
    ngActivityService.deleteAllActivitiesOfAnEntity(accountIdentifier, connectorFQN);
  }
}
