package io.harness.ccm.remote.beans;

import io.harness.ccm.commons.beans.config.CEFeatures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class K8sClusterSetupRequest {
  @Deprecated List<CEFeatures> featuresEnabled;

  // used by VISIBILITY feature
  String connectorIdentifier;
  String orgIdentifier;
  String projectIdentifier;

  // used by OPTIMIZATION feature
  String ccmConnectorIdentifier;
}
