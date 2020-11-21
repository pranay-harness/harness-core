package io.harness.cvng.core.beans;

import io.harness.cvng.beans.DataSourceType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@ApiModel("MonitoringSource")
public class MonitoringSourceDTO {
  String monitoringSourceIdentifier;
  String monitoringSourceName;
  DataSourceType type;
  MonitoringSourceImportStatus importStatus;
  long numberOfServices;
  long importedAt;
}
