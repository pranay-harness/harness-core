package io.harness.ccm.views.graphql.anomalydetection;

import io.harness.ccm.anomaly.entities.AnomalyEntity.AnomaliesDataTableSchema;

import java.util.HashMap;
import java.util.Map;

public class DbFieldMapper {
  Map<String, String> dbFiledMap;

  public DbFieldMapper() {
    dbFiledMap = new HashMap<>();
    dbFiledMap.put("startTime", AnomaliesDataTableSchema.anomalyTime.getColumnNameSQL());

    dbFiledMap.put("awsServicecode", AnomaliesDataTableSchema.awsService.getColumnNameSQL());
    dbFiledMap.put("awsUsageAccountId", AnomaliesDataTableSchema.awsAccount.getColumnNameSQL());

    dbFiledMap.put("gcpProjectId", AnomaliesDataTableSchema.gcpProject.getColumnNameSQL());
    dbFiledMap.put("gcpProduct", AnomaliesDataTableSchema.gcpProduct.getColumnNameSQL());
    dbFiledMap.put("gcpSkuDescription", AnomaliesDataTableSchema.gcpSkuDescription.getColumnNameSQL());
  }

  public String get(String key) {
    return dbFiledMap.get(key);
  }
}
