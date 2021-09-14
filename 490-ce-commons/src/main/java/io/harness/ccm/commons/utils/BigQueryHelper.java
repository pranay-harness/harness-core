/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.commons.utils;

import static java.lang.String.format;

import io.harness.ccm.commons.beans.config.GcpConfig;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class BigQueryHelper {
  @Inject @Named("gcpConfig") GcpConfig config;

  public static final String DATA_SET_NAME_TEMPLATE = "BillingReport_%s";
  public static final String UNIFIED_TABLE = "unifiedTable";
  public static final String INFORMATION_SCHEMA = "INFORMATION_SCHEMA";
  private static final String AWS_RAW_TABLE = "awscur*";
  private static final String GCP_RAW_TABLE = "gcp_billing_export*";
  private static final String AZURE_RAW_TABLE = "azureBilling_%s_%s";

  public String getCloudProviderTableName(String accountId, String tableName) {
    String projectId = config.getGcpProjectId();
    String dataSetId = getDataSetId(accountId);
    return format("%s.%s.%s", projectId, dataSetId, tableName);
  }

  public String getInformationSchemaViewForDataset(String accountId, String view) {
    String projectId = config.getGcpProjectId();
    String dataSetId = getDataSetId(accountId);
    return format("%s.%s.%s.%s", projectId, dataSetId, INFORMATION_SCHEMA, view);
  }

  public String getTableName(String cloudProvider) {
    switch (cloudProvider) {
      case "AWS":
        return AWS_RAW_TABLE;
      case "GCP":
        return GCP_RAW_TABLE;
      case "AZURE":
        return getAzureRawTable();
      default:
        throw new InvalidRequestException("Invalid Cloud Provider");
    }
  }

  private String getAzureRawTable() {
    LocalDateTime localNow = LocalDateTime.now();
    String currentData = localNow.atZone(ZoneId.of("UTC")).toString();
    String[] dateElements = currentData.split("-");
    return format(AZURE_RAW_TABLE, dateElements[0], dateElements[1]);
  }

  public String getGcpProjectId() {
    return config.getGcpProjectId();
  }

  public String getDataSetId(String accountId) {
    return String.format(DATA_SET_NAME_TEMPLATE, modifyStringToComplyRegex(accountId));
  }

  public String modifyStringToComplyRegex(String accountInfo) {
    return accountInfo.toLowerCase().replaceAll("[^a-z0-9]", "_");
  }
}
