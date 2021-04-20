package io.harness.ng.core.entitysetupusage.dto;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(DX)
@Value
@Builder
@TypeAlias("SecretReferredByConnectorSetupUsageDetail")
public class SecretReferredByConnectorSetupUsageDetail implements SetupUsageDetail {
  String fieldName;
}
