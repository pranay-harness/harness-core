package io.harness.connector.mappers.pagerduty;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.pagerduty.PagerDutyConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.pagerduty.PagerDutyConnectorDTO;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(CV)
public class PagerDutyEntityToDTO implements ConnectorEntityToDTOMapper<PagerDutyConnectorDTO, PagerDutyConnector> {
  @Override
  public PagerDutyConnectorDTO createConnectorDTO(PagerDutyConnector connector) {
    return PagerDutyConnectorDTO.builder()
        .apiTokenRef(SecretRefHelper.createSecretRef(connector.getApiTokenRef()))
        .build();
  }
}
