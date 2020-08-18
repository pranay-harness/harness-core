package io.harness.cvng.beans;

import io.harness.cvng.models.VerificationType;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

public abstract class TimeSeriesDataCollectionInfo<T extends ConnectorConfigDTO> extends DataCollectionInfo<T> {
  public VerificationType getVerificationType() {
    return VerificationType.TIME_SERIES;
  }
}
