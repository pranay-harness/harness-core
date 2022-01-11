/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
