/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans;

import io.harness.cvng.models.VerificationType;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

public abstract class TimeSeriesDataCollectionInfo<T extends ConnectorConfigDTO> extends DataCollectionInfo<T> {
  public VerificationType getVerificationType() {
    return VerificationType.TIME_SERIES;
  }
}
