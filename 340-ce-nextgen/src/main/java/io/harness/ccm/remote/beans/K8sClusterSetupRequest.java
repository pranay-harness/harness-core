package io.harness.ccm.remote.beans;

import io.harness.ccm.commons.beans.config.CEFeatures;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class K8sClusterSetupRequest {
  String apiKey;

  @NotEmpty List<CEFeatures> featuresEnabled;

  String connectorIdentifier;
  String orgIdentifier;
  String projectIdentifier;
}
