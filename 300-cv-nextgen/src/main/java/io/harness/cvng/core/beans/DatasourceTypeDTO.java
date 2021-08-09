package io.harness.cvng.core.beans;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.models.VerificationType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DatasourceTypeDTO {
  DataSourceType dataSourceType;
  VerificationType verificationType;
}
