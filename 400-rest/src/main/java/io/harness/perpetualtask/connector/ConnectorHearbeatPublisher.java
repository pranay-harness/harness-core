package io.harness.perpetualtask.connector;

import static io.harness.NGConstants.CONNECTOR_HEARTBEAT_LOG_PREFIX;
import static io.harness.NGConstants.CONNECTOR_STRING;

import io.harness.EntityType;
import io.harness.beans.IdentifierRef;
import io.harness.delegate.beans.connector.ConnectorHeartbeatDelegateResponse;
import io.harness.entityactivity.remote.EntityActivityClient;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.activityhistory.NGActivityStatus;
import io.harness.ng.core.activityhistory.NGActivityType;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import io.harness.utils.IdentifierRefHelper;
import io.harness.utils.RestCallToNGManagerClientUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class ConnectorHearbeatPublisher {
  EntityActivityClient entityActivityClient;
  private static final String CONNECTIVITY_CHECK_DESCRIPTION = "Connectivity Check";

  public void pushConnectivityCheckActivity(
      String accountId, ConnectorHeartbeatDelegateResponse heartbeatDelegateResponse) {
    if (heartbeatDelegateResponse == null) {
      log.error("{} got null delegate heartbeat response in the connector heartbeat for the account {}",
          CONNECTOR_HEARTBEAT_LOG_PREFIX, accountId);
      return;
    }
    NGActivityDTO ngActivityDTO = createConnectivityCheckActivityDTO(heartbeatDelegateResponse);
    try {
      RestCallToNGManagerClientUtils.execute(entityActivityClient.save(ngActivityDTO));
    } catch (Exception ex) {
      log.error("{} Exception while pushing the heartbeat result {}", CONNECTOR_HEARTBEAT_LOG_PREFIX,
          String.format(CONNECTOR_STRING, heartbeatDelegateResponse.getIdentifier(),
              heartbeatDelegateResponse.getAccountIdentifier(), heartbeatDelegateResponse.getOrgIdentifier(),
              heartbeatDelegateResponse.getProjectIdentifier()));
    }
  }

  private NGActivityDTO createConnectivityCheckActivityDTO(
      @NotNull ConnectorHeartbeatDelegateResponse heartbeatDelegateResponse) {
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRefFromEntityIdentifiers(
        heartbeatDelegateResponse.getIdentifier(), heartbeatDelegateResponse.getAccountIdentifier(),
        heartbeatDelegateResponse.getOrgIdentifier(), heartbeatDelegateResponse.getProjectIdentifier());
    EntityDetail referredEntity = EntityDetail.builder()
                                      .type(EntityType.CONNECTORS)
                                      .entityRef(identifierRef)
                                      .name(heartbeatDelegateResponse.getName())
                                      .build();
    NGActivityStatus activityStatus = NGActivityStatus.FAILED;
    if (heartbeatDelegateResponse.getConnectorValidationResult() != null
        && heartbeatDelegateResponse.getConnectorValidationResult().isValid()) {
      activityStatus = NGActivityStatus.SUCCESS;
    }
    return NGActivityDTO.builder()
        .type(NGActivityType.CONNECTIVITY_CHECK)
        .activityStatus(activityStatus)
        .activityTime(heartbeatDelegateResponse.getConnectorValidationResult().getTestedAt())
        .accountIdentifier(heartbeatDelegateResponse.getAccountIdentifier())
        .description(CONNECTIVITY_CHECK_DESCRIPTION)
        .errorMessage(heartbeatDelegateResponse.getConnectorValidationResult().getErrorMessage())
        .referredEntity(referredEntity)
        .build();
  }
}
