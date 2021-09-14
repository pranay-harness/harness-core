/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.cvng.pagerduty;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.pagerduty.PagerDutyConnectorDTO;
import io.harness.delegate.beans.cvng.ConnectorValidationInfo;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CV)
public class PagerDutyConnectorValidationInfo extends ConnectorValidationInfo<PagerDutyConnectorDTO> {
  private static final String DSL =
      readDSL("pagerduty-validation.datacollection", PagerDutyConnectorValidationInfo.class);

  @Override
  public String getConnectionValidationDSL() {
    return DSL;
  }

  @Override
  public String getBaseUrl() {
    return PagerDutyUtils.getBaseUrl();
  }

  @Override
  public Map<String, String> collectionHeaders() {
    return PagerDutyUtils.getCollectionHeaders(getConnectorConfigDTO());
  }

  @Override
  public Map<String, Object> getDslEnvVariables() {
    return new HashMap<>();
  }
}
