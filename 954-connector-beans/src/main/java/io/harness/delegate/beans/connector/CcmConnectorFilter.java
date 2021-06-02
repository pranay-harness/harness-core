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
}
