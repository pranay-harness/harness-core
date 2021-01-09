package io.harness.ng.core.entityactivity.connector;

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

@Singleton
public class ConnectorHeartbeatConsumer {
  @Inject @Named("connectorDecoratorService") ConnectorService connectorService;

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
    ConnectorValidationResult connectorValidationResult =
        ConnectorValidationResult.builder()
            .status(ngActivityDTO.getActivityStatus() == NGActivityStatus.SUCCESS ? ConnectivityStatus.SUCCESS
                                                                                  : ConnectivityStatus.FAILURE)
            .errorSummary(ngActivityDTO.getErrorMessage())
            .testedAt(getCurrentTimeIfActivityTimeIsNull(ngActivityDTO.getActivityTime()))
            .build();
    connectorService.updateConnectivityDetailOfTheConnector(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier, connectorValidationResult);
  }

  private long getCurrentTimeIfActivityTimeIsNull(long activityTime) {
    if (activityTime == 0L) {
      return System.currentTimeMillis();
    }
    return activityTime;
  }
}
