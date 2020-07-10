package io.harness.cvng.beans;

import io.harness.cvng.models.VerificationType;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class SplunkDataCollectionInfo extends DataCollectionInfo {
  private String query;
  private String serviceInstanceIdentifier;
  @Override
  public VerificationType getVerificationType() {
    return VerificationType.LOG;
  }

  @Override
  public Map<String, Object> getDslEnvVariables() {
    Map<String, Object> map = new HashMap<>();
    map.put("query", query);
    map.put("serviceInstanceIdentifier", "$." + serviceInstanceIdentifier);
    // TODO: setting max to 10000 now. We need to find a generic way to throw exception
    // in case of too many logs.
    map.put("maxCount", 10000);
    return map;
  }
}
