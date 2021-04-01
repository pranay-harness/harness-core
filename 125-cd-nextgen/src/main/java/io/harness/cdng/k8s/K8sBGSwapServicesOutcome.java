package io.harness.cdng.k8s;

import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("k8sBGSwapServicesOutcome")
@JsonTypeName("k8sBGSwapServicesOutcome")
public class K8sBGSwapServicesOutcome implements Outcome {
  @Override
  public String getType() {
    return "k8sBGSwapServicesOutcome";
  }
}