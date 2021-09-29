package io.harness.connector.mappers.pagerduty;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.pagerduty.PagerDutyConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.pagerduty.PagerDutyConnectorDTO;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(CV)
public class PagerDutyDTOToEntity implements ConnectorDTOToEntityMapper<PagerDutyConnectorDTO, PagerDutyConnector> {
  @Override
  public PagerDutyConnector toConnectorEntity(PagerDutyConnectorDTO connectorDTO) {
    return PagerDutyConnector.builder()
        .apiTokenRef(SecretRefHelper.getSecretConfigString(connectorDTO.getApiTokenRef()))
        .build();
  }
}
