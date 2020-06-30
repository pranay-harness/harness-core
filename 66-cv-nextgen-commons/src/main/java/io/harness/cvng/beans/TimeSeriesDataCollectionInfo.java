package io.harness.cvng.beans;

import io.harness.cvng.models.VerificationType;

public abstract class TimeSeriesDataCollectionInfo extends DataCollectionInfo {
  public VerificationType getVerificationType() {
    return VerificationType.TIME_SERIES;
  }
}
