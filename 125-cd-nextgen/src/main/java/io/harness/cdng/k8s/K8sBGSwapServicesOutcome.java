package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Value
@Builder
@TypeAlias("k8sBGSwapServicesOutcome")
@JsonTypeName("k8sBGSwapServicesOutcome")
@RecasterAlias("io.harness.cdng.k8s.K8sBGSwapServicesOutcome")
public class K8sBGSwapServicesOutcome implements Outcome {}