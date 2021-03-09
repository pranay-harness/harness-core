package io.harness.ccm.views.graphql.anomalydetection;

import java.util.HashMap;
import java.util.Map;

public class DbFieldMapper {
  Map<String, String> dbFiledMap;

  public DbFieldMapper() {
    dbFiledMap = new HashMap<>();
    dbFiledMap.put("awsServicecode", "awsservice");
    dbFiledMap.put("awsUsageAccountId", "awsaccount");
    dbFiledMap.put("startTime", "anomalytime");
  }

  public String get(String key) {
    return dbFiledMap.get(key);
  }
}
