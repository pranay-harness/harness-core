package io.harness.cdng.k8s;

import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("k8sRollingOutcome")
@JsonTypeName("k8sRollingOutcome")
public class K8sRollingOutcome implements Outcome {
  String releaseName;
  int releaseNumber;

  @Override
  public String getType() {
    return "k8sRollingOutcome";
  }
}
