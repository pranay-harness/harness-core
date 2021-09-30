package io.harness.delegate.beans.connector.k8Connector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.CEFeatures;

import java.util.List;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@OwnedBy(HarnessTeam.CE)
public class CEK8sValidationParams extends K8sValidationParams {
  List<CEFeatures> featuresEnabled;
}
