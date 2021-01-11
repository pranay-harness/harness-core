package io.harness.ng.core.entityactivity.connector;

import static io.harness.NGConstants.CONNECTOR_STRING;
import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;

import io.harness.EntityType;
import io.harness.beans.IdentifierRef;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectivityStatus;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.activityhistory.NGActivityStatus;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ConnectorHeartbeatConsumer {
  @Inject @Named(DEFAULT_CONNECTOR_SERVICE) ConnectorService connectorService;

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
    ConnectorValidationResult connectorValidationResult =
        ConnectorValidationResult.builder()
            .status(ngActivityDTO.getActivityStatus() == NGActivityStatus.SUCCESS ? ConnectivityStatus.SUCCESS
                                                                                  : ConnectivityStatus.FAILURE)
            .errorSummary(ngActivityDTO.getErrorMessage())
            .testedAt(getCurrentTimeIfActivityTimeIsNull(ngActivityDTO.getActivityTime()))
            .build();
    connectorService.updateConnectivityDetailOfTheConnector(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier, connectorValidationResult);
    log.info("Completed Updating the connector heartbeat result for the connector {}", connectorMessage);
  }

  private long getCurrentTimeIfActivityTimeIsNull(long activityTime) {
    if (activityTime == 0L) {
      return System.currentTimeMillis();
    }
    return activityTime;
  }
}
