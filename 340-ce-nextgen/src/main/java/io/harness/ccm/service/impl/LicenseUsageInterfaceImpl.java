package io.harness.ccm.service.impl;

import static java.lang.String.format;

import io.harness.ModuleType;
import io.harness.ccm.CENextGenConfiguration;
import io.harness.ccm.bigQuery.BigQueryService;
import io.harness.ccm.commons.beans.usage.CELicenseUsageDTO;
import io.harness.licensing.usage.beans.UsageDataDTO;
import io.harness.licensing.usage.interfaces.LicenseUsageInterface;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
import com.google.inject.Inject;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LicenseUsageInterfaceImpl implements LicenseUsageInterface<CELicenseUsageDTO> {
  @Inject BigQueryService bigQueryService;
  @Inject CENextGenConfiguration configuration;

  public static final String DATA_SET_NAME = "CE_INTERNAL";
  public static final String TABLE_NAME = "costAggregated";
  public static final String QUERY_TEMPLATE =
      "SELECT SUM(cost) FROM `%s` WHERE cloudProvider IN ('AWS','GCP','AZURE') AND day >= TIMESTAMP_MILLIS(%s) AND accountId = %s";

  private final Cache<CacheKey, CELicenseUsageDTO> licenseUsageCache =
      Caffeine.newBuilder().expireAfterWrite(8, TimeUnit.HOURS).build();

  @Value
  @AllArgsConstructor
  private static class CacheKey {
    private String accountId;
    private Long timestamp;
  }

  @Override
  public CELicenseUsageDTO getLicenseUsage(String accountIdentifier, ModuleType module, long timestamp) {
    CacheKey cacheKey = new CacheKey(accountIdentifier, timestamp);
    CELicenseUsageDTO cachedCELicenseUsageDTO = licenseUsageCache.getIfPresent(cacheKey);
    if (null != cachedCELicenseUsageDTO) {
      return cachedCELicenseUsageDTO;
    }

    Long activeSpend = getActiveSpend(timestamp, accountIdentifier);
    CELicenseUsageDTO ceLicenseUsageDTO =
        CELicenseUsageDTO.builder()
            .activeSpend(UsageDataDTO.builder().count(activeSpend).displayName("").build())
            .timestamp(timestamp)
            .accountIdentifier(accountIdentifier)
            .build();
    licenseUsageCache.put(cacheKey, ceLicenseUsageDTO);
    return ceLicenseUsageDTO;
  }

  private Long getActiveSpend(long timestamp, String accountIdentifier) {
    String gcpProjectId = configuration.getGcpConfig().getGcpProjectId();
    String cloudProviderTableName = format("%s.%s.%s", gcpProjectId, DATA_SET_NAME, TABLE_NAME);
    String query = format(QUERY_TEMPLATE, cloudProviderTableName, timestamp, accountIdentifier);

    BigQuery bigQuery = bigQueryService.get();
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
    TableResult result;
    try {
      result = bigQuery.query(queryConfig);
    } catch (InterruptedException e) {
      log.error("Failed to getActiveSpend for Account:{}, {}", accountIdentifier, e);
      Thread.currentThread().interrupt();
      return null;
    }

    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();
    Long cost = 0L;

    for (FieldValueList row : result.iterateAll()) {
      for (Field field : fields) {
        switch (field.getType().getStandardType()) {
          case FLOAT64:
            cost = getNumericValue(row, field);
            break;
          default:
            break;
        }
      }
    }
    return cost;
  }

  private long getNumericValue(FieldValueList row, Field field) {
    FieldValue value = row.get(field.getName());
    return Math.round(value.getNumericValue().longValue() * 100L) / 100L;
  }
}