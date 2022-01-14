package io.harness.cvng.beans.splunk;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.DataCollectionRequestType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeName("SPLUNK_SAVED_SEARCHES")
@SuperBuilder
@NoArgsConstructor
@OwnedBy(CV)
public class SplunkSavedSearchRequest extends SplunkDataCollectionRequest {
  public static final String DSL =
      SplunkDataCollectionRequest.readDSL("splunk-saved-searches.datacollection", SplunkSavedSearchRequest.class);

  @Override
  public String getDSL() {
    return DSL;
  }

  @Override
  public DataCollectionRequestType getType() {
    return DataCollectionRequestType.SPLUNK_SAVED_SEARCHES;
  }
}
