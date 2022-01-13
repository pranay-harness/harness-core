/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.service.impl;

import static java.lang.String.format;

import io.harness.ModuleType;
import io.harness.ccm.CENextGenConfiguration;
import io.harness.ccm.bigQuery.BigQueryService;
import io.harness.ccm.commons.beans.usage.CELicenseUsageDTO;
import io.harness.licensing.usage.beans.UsageDataDTO;
import io.harness.licensing.usage.interfaces.LicenseUsageInterface;
import io.harness.licensing.usage.params.UsageRequestParams;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LicenseUsageInterfaceImpl implements LicenseUsageInterface<CELicenseUsageDTO, UsageRequestParams> {
  @Inject BigQueryService bigQueryService;
  @Inject CENextGenConfiguration configuration;

  public static final String ACCOUNT_ID = "accountId";
  public static final String COST = "cost";
  public static final String MONTH = "month";
  public static final String CLOUD_PROVIDER = "cloudProvider";
  private static final Set<String> duplicateK8sCloudProviders = ImmutableSet.of("K8S_AWS", "K8S_AZURE", "K8S_GCP");

  public static final String DATA_SET_NAME = "CE_INTERNAL";
  public static final String TABLE_NAME = "costAggregated";
  public static final String QUERY_TEMPLATE =
      "SELECT SUM(cost) AS cost, TIMESTAMP_TRUNC(day, month) AS month, cloudProvider FROM `%s` "
      + "WHERE day >= TIMESTAMP_MILLIS(%s) AND day <= TIMESTAMP_MILLIS(%s) AND accountId = '%s' GROUP BY month, cloudProvider";

  private final Cache<CacheKey, CELicenseUsageDTO> licenseUsageCache =
      Caffeine.newBuilder().expireAfterWrite(8, TimeUnit.HOURS).build();

  @Value
  @AllArgsConstructor
  private static class CacheKey {
    private String accountId;
    private Long timestamp;
  }

  @Override
  public CELicenseUsageDTO getLicenseUsage(
      String accountIdentifier, ModuleType module, long timestamp, UsageRequestParams usageRequest) {
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
    long endOfDay = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).toEpochMilli();
    String query = format(QUERY_TEMPLATE, cloudProviderTableName, timestamp, endOfDay, accountIdentifier);

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

    Long cost = 0L;
    Multimap<String, String> multiMap = ArrayListMultimap.create();

    // Will maintain a multi map with Month - [Cloud Provider] mapping
    for (FieldValueList row : result.iterateAll()) {
      multiMap.put(row.get(MONTH).getStringValue(), row.get(CLOUD_PROVIDER).getStringValue());
    }

    for (FieldValueList row : result.iterateAll()) {
      String cloudProvider = row.get(CLOUD_PROVIDER).getStringValue();
      if (duplicateK8sCloudProviders.contains(cloudProvider)) {
        // K8S_AWS, K8S_AWS, K8S_AWS  => cloudProviderSlices[0] -> K8S | cloudProviderSlices[1] -> AWS/GCP/AZURE
        String cloudProviderSlices[] = cloudProvider.split("_");
        String month = row.get(MONTH).getStringValue();

        // If Corresponding Cloud Provider Data is present -> We should not add the K8s Cost
        if (!multiMap.containsEntry(month, cloudProviderSlices[1])) {
          cost += getNumericValue(row, COST);
        }
      } else {
        cost += getNumericValue(row, COST);
      }
    }
    return Math.round(cost * 100L) / 100L;
  }

  private long getNumericValue(FieldValueList row, String fieldName) {
    FieldValue value = row.get(fieldName);
    return Math.round(value.getNumericValue().longValue() * 100L) / 100L;
  }
}
