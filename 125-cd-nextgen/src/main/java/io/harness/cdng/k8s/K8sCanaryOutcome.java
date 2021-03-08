package io.harness.cdng.k8s;

import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("k8sCanaryOutcome")
@JsonTypeName("k8sCanaryOutcome")
public class K8sCanaryOutcome implements Outcome {
  String releaseName;
  Integer targetInstances;
  Integer releaseNumber;
  String canaryWorkload;

  @Override
  public String getType() {
    return "k8sCanaryOutcome";
  }
}
