package io.harness.connector.utils;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.entities.embedded.gcpccm.GcpBillingExportDetails;
import io.harness.connector.entities.embedded.gcpccm.GcpCloudCostConfig;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.gcpccm.GcpBillingExportSpecDTO;
import io.harness.delegate.beans.connector.gcpccm.GcpCloudCostConnectorDTO;
import io.harness.delegate.beans.connector.gcpccm.GcpCloudCostFeatures;

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

  List<GcpCloudCostFeatures> featuresEnabled = ImmutableList.of(GcpCloudCostFeatures.BILLING);

  public GcpCloudCostConnectorDTO createGcpCcmConnectorDTO() {
    return GcpCloudCostConnectorDTO.builder()
        .featuresEnabled(featuresEnabled)
        .billingExportSpec(GcpBillingExportSpecDTO.builder().datasetId(datasetId).projectId(projectId).build())
        .build();
  }

  public ConnectorDTO createConnectorDTO() {
    return CommonTestHelper.createConnectorDTO(ConnectorType.GCP_CLOUD_COST, createGcpCcmConnectorDTO());
  }

  public GcpCloudCostConfig createGcpCcmConfig() {
    return GcpCloudCostConfig.builder()
        .featuresEnabled(featuresEnabled)
        .billingExportDetails(GcpBillingExportDetails.builder().datasetId(datasetId).projectId(projectId).build())
        .build();
  }
}
