package io.harness.cvng.beans.splunk;

import io.harness.cvng.beans.DataCollectionRequestType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.experimental.SuperBuilder;

@JsonTypeName("SPLUNK_SAVED_SEARCHES")
@SuperBuilder
public class SplunkSavedSearchRequest extends SplunkDataCollectionRequest {
  public static final String DSL =
      SplunkDataCollectionRequest.readDSL("splunk-saved-searches.datacollection", SplunkSavedSearchRequest.class);
  public SplunkSavedSearchRequest() {
    setType(DataCollectionRequestType.SPLUNK_SAVED_SEARCHES);
  }
  @Override
  public String getDSL() {
    return DSL;
  }
}
