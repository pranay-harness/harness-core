package io.harness.cvng.beans;

import io.harness.cvng.models.VerificationType;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

import lombok.Data;

@Data
public abstract class LogDataCollectionInfo<T extends ConnectorConfigDTO> extends DataCollectionInfo<T> {
  private String hostCollectionDSL;
  @Override
  public VerificationType getVerificationType() {
    return VerificationType.LOG;
  }
}
