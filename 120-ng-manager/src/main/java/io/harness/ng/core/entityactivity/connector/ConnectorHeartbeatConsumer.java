package io.harness.ng.core.entityactivity.connector;

import static io.harness.NGConstants.CONNECTOR_STRING;
import static io.harness.ng.NextGenModule.CONNECTOR_DECORATOR_SERVICE;

import io.harness.EntityType;
import io.harness.beans.IdentifierRef;
import io.harness.connector.services.ConnectorService;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.activityhistory.dto.ConnectivityCheckActivityDetailDTO;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ConnectorHeartbeatConsumer {
  @Inject @Named(CONNECTOR_DECORATOR_SERVICE) ConnectorService connectorService;

  public void saveTheConnectivityStatus(NGActivityDTO ngActivityDTO) {
    String accountIdentifier = ngActivityDTO.getAccountIdentifier();
    EntityDetail connectorDetails = ngActivityDTO.getReferredEntity();
    if (connectorDetails.getType() != EntityType.CONNECTORS
        || !(connectorDetails.getEntityRef() instanceof IdentifierRef)) {
      return;
    }
    IdentifierRef entityRef = (IdentifierRef) connectorDetails.getEntityRef();
    String orgIdentifier = entityRef.getOrgIdentifier();
    String projectIdentifier = entityRef.getProjectIdentifier();
    String connectorIdentifier = entityRef.getIdentifier();
    String connectorMessage = String.format(
        CONNECTOR_STRING, connectorIdentifier, ngActivityDTO.getAccountIdentifier(), orgIdentifier, projectIdentifier);
    log.info("Updating the connector heartbeat result for the connector {}", connectorMessage);
    ConnectivityCheckActivityDetailDTO connectivityCheckActivityDetail =
        (ConnectivityCheckActivityDetailDTO) ngActivityDTO.getDetail();
    connectorService.updateConnectivityDetailOfTheConnector(accountIdentifier, orgIdentifier, projectIdentifier,
        connectorIdentifier, connectivityCheckActivityDetail.getConnectorValidationResult());
    log.info("Completed Updating the connector heartbeat result for the connector {}", connectorMessage);
  }
}
