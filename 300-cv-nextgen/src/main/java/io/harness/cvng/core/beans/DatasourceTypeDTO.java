/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
