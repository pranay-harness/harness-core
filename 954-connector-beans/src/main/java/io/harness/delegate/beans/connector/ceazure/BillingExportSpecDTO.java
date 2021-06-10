package io.harness.delegate.beans.connector.ceazure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
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
public class BillingExportSpecDTO {
  @NotNull String storageAccountName;
  @NotNull String containerName;
  @NotNull String directoryName;
  @NotNull String reportName;
}
