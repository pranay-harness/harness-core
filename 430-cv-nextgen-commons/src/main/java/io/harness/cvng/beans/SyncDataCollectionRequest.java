package io.harness.cvng.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.Instant;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@JsonTypeName("SYNC_DATA_COLLECTION")
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.CV)
public class SyncDataCollectionRequest extends DataCollectionRequest<ConnectorConfigDTO> {
  DataCollectionInfo dataCollectionInfo;
  Instant startTime;
  Instant endTime;

  public SyncDataCollectionRequest() {
    setType(DataCollectionRequestType.SYNC_DATA_COLLECTION);
  }

  @Override
  public String getDSL() {
    return dataCollectionInfo.getDataCollectionDsl();
  }

  @Override
  public String getBaseUrl() {
    return dataCollectionInfo.getBaseUrl(getConnectorConfigDTO());
  }

  @Override
  public Map<String, String> collectionHeaders() {
    return dataCollectionInfo.collectionHeaders(getConnectorConfigDTO());
  }

  public Map<String, String> collectionParams() {
    return dataCollectionInfo.collectionParams(getConnectorConfigDTO());
  }

  public Map<String, Object> fetchDslEnvVariables() {
    Map<String, Object> dslVariables = dataCollectionInfo.getDslEnvVariables(getConnectorConfigDTO());
    dslVariables.put("collectHostData", dataCollectionInfo.isCollectHostData());
    dslVariables.put("startTimeMillis", startTime.toEpochMilli());
    dslVariables.put("endTimeMillis", endTime.toEpochMilli());
    return dslVariables;
  }
}
