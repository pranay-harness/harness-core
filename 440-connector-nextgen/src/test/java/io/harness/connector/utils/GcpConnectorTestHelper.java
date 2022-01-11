/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.utils;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.entities.embedded.gcpccm.GcpBillingExportDetails;
import io.harness.connector.entities.embedded.gcpccm.GcpCloudCostConfig;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.gcpccm.GcpBillingExportSpecDTO;
import io.harness.delegate.beans.connector.gcpccm.GcpCloudCostConnectorDTO;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;

@UtilityClass
@FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
@OwnedBy(CE)
public class GcpConnectorTestHelper {
  String projectId = "projectId";
  String datasetId = "datasetId";
  String tableId = "tableId";
  String serviceAccountEmail = "serviceAccountEmail";

  List<CEFeatures> featuresEnabled =
      ImmutableList.of(CEFeatures.BILLING, CEFeatures.OPTIMIZATION, CEFeatures.VISIBILITY);

  public GcpCloudCostConnectorDTO createGcpCcmConnectorDTO() {
    return GcpCloudCostConnectorDTO.builder()
        .featuresEnabled(featuresEnabled)
        .projectId(projectId)
        .serviceAccountEmail(serviceAccountEmail)
        .billingExportSpec(GcpBillingExportSpecDTO.builder().datasetId(datasetId).tableId(tableId).build())
        .build();
  }

  public ConnectorDTO createConnectorDTO() {
    return CommonTestHelper.createConnectorDTO(ConnectorType.GCP_CLOUD_COST, createGcpCcmConnectorDTO());
  }

  public GcpCloudCostConfig createGcpCcmConfig() {
    return GcpCloudCostConfig.builder()
        .featuresEnabled(featuresEnabled)
        .projectId(projectId)
        .serviceAccountEmail(serviceAccountEmail)
        .billingExportDetails(GcpBillingExportDetails.builder().datasetId(datasetId).tableId(tableId).build())
        .build();
  }
}
