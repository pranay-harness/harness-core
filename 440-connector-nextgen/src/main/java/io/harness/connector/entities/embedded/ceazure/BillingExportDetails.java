package io.harness.connector.entities.embedded.ceazure;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@FieldNameConstants(innerTypeName = "BillingExportDetailsKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("io.harness.connector.entities.embedded.ceazure.BillingExportDetails")
@OwnedBy(DX)
public class BillingExportDetails {
  @NotNull String storageAccountName;
  @NotNull String containerName;
  @NotNull String directoryName;
}
