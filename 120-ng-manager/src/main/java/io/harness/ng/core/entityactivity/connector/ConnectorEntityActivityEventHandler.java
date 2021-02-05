package io.harness.ng.core.entityactivity.connector;

import static io.harness.NGConstants.CONNECTOR_STRING;
import static io.harness.ng.NextGenModule.CONNECTOR_DECORATOR_SERVICE;

import io.harness.EntityType;
import io.harness.beans.IdentifierRef;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.services.ConnectorService;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.activityhistory.dto.ConnectivityCheckActivityDetailDTO;
import io.harness.ng.core.activityhistory.dto.EntityUsageActivityDetailDTO;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class ConnectorEntityActivityEventHandler {
  @Inject @Named(CONNECTOR_DECORATOR_SERVICE) private ConnectorService connectorService;

  public void updateActivityResultInConnectors(NGActivityDTO ngActivityDTO) {
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
    log.info("Updating the connector activity result for the connector {}", connectorMessage);
    Long activityTime = null;
    ConnectorValidationResult connectorValidationResult = null;
    switch (ngActivityDTO.getType()) {
      case CONNECTIVITY_CHECK:
        ConnectivityCheckActivityDetailDTO connectivityCheckActivityDetail =
            (ConnectivityCheckActivityDetailDTO) ngActivityDTO.getDetail();
        connectorValidationResult = connectivityCheckActivityDetail.getConnectorValidationResult();
        break;
      case ENTITY_USAGE:
        EntityUsageActivityDetailDTO entityUsageActivityDetailDTO =
            (EntityUsageActivityDetailDTO) ngActivityDTO.getDetail();
        activityTime = ngActivityDTO.getActivityTime();
        connectorValidationResult =
            createConnectorValidatonResultFromEntityUsage(entityUsageActivityDetailDTO, activityTime);
        break;
      case ENTITY_UPDATE:
      case ENTITY_CREATION:
        activityTime = ngActivityDTO.getActivityTime();
        break;
      default:
    }
    connectorService.updateActivityDetailsInTheConnector(accountIdentifier, orgIdentifier, projectIdentifier,
        connectorIdentifier, connectorValidationResult, activityTime);
    log.info("Completed Updating the connector heartbeat result for the connector {}", connectorMessage);
  }

  private ConnectorValidationResult createConnectorValidatonResultFromEntityUsage(
      EntityUsageActivityDetailDTO entityUsageActivityDetailDTO, Long activityTime) {
    return ConnectorValidationResult.builder()
        .status(entityUsageActivityDetailDTO.getStatus())
        .errorSummary(entityUsageActivityDetailDTO.getErrorSummary())
        .errors(entityUsageActivityDetailDTO.getErrors())
        .testedAt(activityTime)
        .build();
  }
}
