package io.harness.delegate.beans.connector.ceazure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("BillingExportSpec")
@Schema(name = "BillingExportSpec",
    description = "Returns Billing details like StorageAccount's Name,"
        + " container's Name, directory's Name, report Name and subscription Id")
public class BillingExportSpecDTO {
  @NotNull String storageAccountName;
  @NotNull String containerName;
  @NotNull String directoryName;
  @NotNull String reportName;
  @NotNull String subscriptionId;
}
