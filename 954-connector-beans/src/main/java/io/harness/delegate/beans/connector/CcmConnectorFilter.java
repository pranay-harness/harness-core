/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@OwnedBy(HarnessTeam.CE)
public class CcmConnectorFilter {
  List<CEFeatures> featuresEnabled;
  String awsAccountId;
  String azureSubscriptionId;
  String azureTenantId;
  String gcpProjectId;
  String k8sConnectorRef;
}
