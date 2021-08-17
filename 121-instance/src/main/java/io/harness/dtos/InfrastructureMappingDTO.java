package io.harness.dtos;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@OwnedBy(HarnessTeam.DX)
@Data
@Builder
public class InfrastructureMappingDTO {
  @Nullable private String id;
  @NonNull private String accountIdentifier;
  @NonNull private String orgIdentifier;
  @NonNull private String projectIdentifier;
  @NonNull private String infrastructureKind;
  @NonNull private String connectorRef;
  @NonNull private String envIdentifier;
  @NonNull private String serviceIdentifier;
  @NonNull private String infrastructureKey;
}
