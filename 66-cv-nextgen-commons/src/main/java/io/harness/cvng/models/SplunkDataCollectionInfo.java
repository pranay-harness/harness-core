package io.harness.cvng.models;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;

@Builder
@JsonTypeName("SPLUNK")
public class SplunkDataCollectionInfo extends LogDataCollectionInfo {
  private String connectorId;
  private SplunkConnector splunkConnector;
  private String query;

  public String getType() {
    return "SPLUNK"; // TODO: move this to enum
  }

  @Override
  public Connector getConnector() {
    return splunkConnector;
  }
}
