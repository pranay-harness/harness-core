package io.harness.connector.entities.embedded.ceazure;

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
public class BillingExportDetails {
  @NotNull String storageAccountName;
  @NotNull String containerName;
  @NotNull String directoryName;
  @NotNull String reportName;
}
