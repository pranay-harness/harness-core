package io.harness.batch.processing.pricing.gcp.bigquery.impl;

import static io.harness.batch.processing.pricing.gcp.bigquery.BigQueryConstants.computeProductFamily;
import static io.harness.batch.processing.pricing.gcp.bigquery.BigQueryConstants.cost;
import static io.harness.batch.processing.pricing.gcp.bigquery.BigQueryConstants.effectiveCost;
import static io.harness.batch.processing.pricing.gcp.bigquery.BigQueryConstants.eksCpuInstanceType;
import static io.harness.batch.processing.pricing.gcp.bigquery.BigQueryConstants.eksMemoryInstanceType;
import static io.harness.batch.processing.pricing.gcp.bigquery.BigQueryConstants.eksNetworkInstanceType;
import static io.harness.batch.processing.pricing.gcp.bigquery.BigQueryConstants.networkProductFamily;
import static io.harness.batch.processing.pricing.gcp.bigquery.BigQueryConstants.productFamily;
import static io.harness.batch.processing.pricing.gcp.bigquery.BigQueryConstants.resourceId;
import static io.harness.batch.processing.pricing.gcp.bigquery.BigQueryConstants.serviceCode;
import static io.harness.batch.processing.pricing.gcp.bigquery.BigQueryConstants.usageType;
import static io.harness.ccm.billing.GcpServiceAccountServiceImpl.getCredentials;

import static java.lang.String.format;

import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.config.BillingDataPipelineConfig;
import io.harness.batch.processing.pricing.data.VMInstanceBillingData;
import io.harness.batch.processing.pricing.data.VMInstanceServiceBillingData;
import io.harness.batch.processing.pricing.data.VMInstanceServiceBillingData.VMInstanceServiceBillingDataBuilder;
import io.harness.batch.processing.pricing.gcp.bigquery.BigQueryConstants;
import io.harness.batch.processing.pricing.gcp.bigquery.BigQueryHelperService;

import software.wings.beans.ce.CEMetadataRecord.CEMetadataRecordBuilder;
import software.wings.graphql.datafetcher.billing.CloudBillingHelper;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BigQueryHelperServiceImpl implements BigQueryHelperService {
  private BatchMainConfig mainConfig;
  private CloudBillingHelper cloudBillingHelper;

  private static final String preAggregated = "preAggregated";
  private static final String countConst = "count";
  private static final String cloudProviderConst = "cloudProvider";

  @Autowired
  public BigQueryHelperServiceImpl(BatchMainConfig mainConfig, CloudBillingHelper cloudBillingHelper) {
    this.mainConfig = mainConfig;
    this.cloudBillingHelper = cloudBillingHelper;
  }

  private static final String GOOGLE_CREDENTIALS_PATH = "GOOGLE_CREDENTIALS_PATH";
  private static final String AWS_CUR_TABLE_NAME = "awscur_%s_%s";
  private String resourceCondition = "resourceid like '%%%s%%'";

  @Override
  public Map<String, VMInstanceBillingData> getAwsEC2BillingData(
      List<String> resourceId, Instant startTime, Instant endTime, String dataSetId) {
    String query = BigQueryConstants.AWS_EC2_BILLING_QUERY;
    String resourceIds = String.join("','", resourceId);
    String projectTableName = getAwsProjectTableName(startTime, dataSetId);
    String formattedQuery = format(query, projectTableName, resourceIds, startTime, endTime);
    return query(formattedQuery);
  }

  @Override
  public Map<String, VMInstanceBillingData> getEKSFargateBillingData(
      List<String> resourceIds, Instant startTime, Instant endTime, String dataSetId) {
    String query = BigQueryConstants.EKS_FARGATE_BILLING_QUERY;
    String projectTableName = getAwsProjectTableName(startTime, dataSetId);
    String formattedQuery =
        format(query, projectTableName, getResourceConditionWhereClause(resourceIds), startTime, endTime);
    log.info("EKS Fargate Billing Query: {}", formattedQuery);
    return queryEKSFargate(formattedQuery);
  }

  private String getResourceConditionWhereClause(List<String> resourceIds) {
    List<String> resourceIdConditions = new ArrayList<>();
    for (String resourceId : resourceIds) {
      resourceIdConditions.add(format(resourceCondition, resourceId));
    }
    return String.join(" OR ", resourceIdConditions);
  }

  private Map<String, VMInstanceBillingData> query(String formattedQuery) {
    log.info("Formatted query {}", formattedQuery);
    BigQuery bigQueryService = getBigQueryService();
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(formattedQuery).build();
    TableResult result = null;
    try {
      result = bigQueryService.query(queryConfig);
      return convertToAwsInstanceBillingData(result);
    } catch (InterruptedException e) {
      log.error("Failed to get AWS EC2 Billing Data. {}", e);
      Thread.currentThread().interrupt();
    }
    return Collections.emptyMap();
  }

  private Map<String, VMInstanceBillingData> queryEKSFargate(String formattedQuery) {
    BigQuery bigQueryService = getBigQueryService();
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(formattedQuery).build();
    TableResult result = null;
    try {
      result = bigQueryService.query(queryConfig);
      return convertToEksFargateInstanceBillingData(result);
    } catch (InterruptedException e) {
      log.error("Failed to get EKS Fargate Data from CUR. {}", e);
      Thread.currentThread().interrupt();
    }
    return Collections.emptyMap();
  }

  private String getAwsCurTableName(Instant startTime) {
    Date date = Date.from(startTime);
    SimpleDateFormat monthFormatter = new SimpleDateFormat("MM");
    String month = monthFormatter.format(date);
    SimpleDateFormat yearFormatter = new SimpleDateFormat("yyyy");
    String year = yearFormatter.format(date);
    return format(AWS_CUR_TABLE_NAME, year, month);
  }

  private String getAwsProjectTableName(Instant startTime, String dataSetId) {
    String tableName = getAwsCurTableName(startTime);
    BillingDataPipelineConfig billingDataPipelineConfig = mainConfig.getBillingDataPipelineConfig();
    return format("%s.%s.%s", billingDataPipelineConfig.getGcpProjectId(), dataSetId, tableName);
  }

  @Override
  public Map<String, VMInstanceBillingData> getAwsBillingData(Instant startTime, Instant endTime, String dataSetId) {
    String query = BigQueryConstants.AWS_BILLING_DATA;
    String projectTableName = getAwsProjectTableName(startTime, dataSetId);
    String formattedQuery = format(query, projectTableName, startTime, endTime);
    return query(formattedQuery);
  }

  @Override
  public void updateCloudProviderMetaData(String accountId, CEMetadataRecordBuilder ceMetadataRecordBuilder) {
    String tableName = cloudBillingHelper.getCloudProviderTableName(
        mainConfig.getBillingDataPipelineConfig().getGcpProjectId(), accountId, preAggregated);
    String formattedQuery = format(BigQueryConstants.CLOUD_PROVIDER_AGG_DATA, tableName);
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(formattedQuery).build();
    try {
      TableResult result = getBigQueryService().query(queryConfig);
      for (FieldValueList row : result.iterateAll()) {
        String cloudProvider = row.get(cloudProviderConst).getStringValue();
        switch (cloudProvider) {
          case "AWS":
            ceMetadataRecordBuilder.awsDataPresent(row.get(countConst).getDoubleValue() > 0);
            break;
          case "GCP":
            ceMetadataRecordBuilder.gcpDataPresent(row.get(countConst).getDoubleValue() > 0);
            break;
          case "AZURE":
            ceMetadataRecordBuilder.azureDataPresent(row.get(countConst).getDoubleValue() > 0);
            break;
          default:
            break;
        }
      }

    } catch (InterruptedException e) {
      log.error("Failed to get CloudProvider overview data. {}", e);
      Thread.currentThread().interrupt();
    }
  }

  public FieldList getFieldList(TableResult result) {
    Schema schema = result.getSchema();
    return schema.getFields();
  }

  public Iterable<FieldValueList> getFieldValueLists(TableResult result) {
    return result.iterateAll();
  }

  private List<VMInstanceServiceBillingData> convertToAwsInstanceServiceBillingData(TableResult result) {
    FieldList fields = getFieldList(result);
    List<VMInstanceServiceBillingData> instanceServiceBillingDataList = new ArrayList<>();
    Iterable<FieldValueList> fieldValueLists = getFieldValueLists(result);
    for (FieldValueList row : fieldValueLists) {
      VMInstanceServiceBillingDataBuilder dataBuilder = VMInstanceServiceBillingData.builder();
      for (Field field : fields) {
        switch (field.getName()) {
          case resourceId:
            dataBuilder.resourceId(getIdFromArn(fetchStringValue(row, field)));
            break;
          case serviceCode:
            dataBuilder.serviceCode(fetchStringValue(row, field));
            break;
          case usageType:
            dataBuilder.usageType(fetchStringValue(row, field));
            break;
          case productFamily:
            dataBuilder.productFamily(fetchStringValue(row, field));
            break;
          case cost:
            dataBuilder.cost(getNumericValue(row, field));
            break;
          case effectiveCost:
            dataBuilder.effectiveCost(getDoubleValue(row, field));
            break;
          default:
            break;
        }
      }
      instanceServiceBillingDataList.add(dataBuilder.build());
    }
    log.info("Resource Id data {} ", instanceServiceBillingDataList);
    return instanceServiceBillingDataList;
  }

  private String getIdFromArn(String arn) {
    return arn.substring(arn.lastIndexOf('/') + 1);
  }

  private Map<String, VMInstanceBillingData> convertToAwsInstanceBillingData(TableResult result) {
    List<VMInstanceServiceBillingData> vmInstanceServiceBillingDataList =
        convertToAwsInstanceServiceBillingData(result);
    Map<String, VMInstanceBillingData> vmInstanceBillingDataMap = new HashMap<>();
    vmInstanceServiceBillingDataList.forEach(vmInstanceServiceBillingData -> {
      String resourceId = vmInstanceServiceBillingData.getResourceId();
      VMInstanceBillingData vmInstanceBillingData = VMInstanceBillingData.builder().resourceId(resourceId).build();
      if (vmInstanceBillingDataMap.containsKey(resourceId)) {
        vmInstanceBillingData = vmInstanceBillingDataMap.get(resourceId);
      }

      if (networkProductFamily.equals(vmInstanceServiceBillingData.getProductFamily())) {
        vmInstanceBillingData =
            vmInstanceBillingData.toBuilder().networkCost(vmInstanceServiceBillingData.getCost()).build();
      }

      if (computeProductFamily.equals(vmInstanceServiceBillingData.getProductFamily())
          || vmInstanceServiceBillingData.getProductFamily() == null) {
        double cost = vmInstanceServiceBillingData.getCost();
        if (null != vmInstanceServiceBillingData.getEffectiveCost()) {
          cost = vmInstanceServiceBillingData.getEffectiveCost();
        }
        vmInstanceBillingData = vmInstanceBillingData.toBuilder().computeCost(cost).build();
      }

      vmInstanceBillingDataMap.put(resourceId, vmInstanceBillingData);
    });

    log.info("resource map data {} ", vmInstanceBillingDataMap);
    return vmInstanceBillingDataMap;
  }

  private Map<String, VMInstanceBillingData> convertToEksFargateInstanceBillingData(TableResult result) {
    List<VMInstanceServiceBillingData> vmInstanceServiceBillingDataList =
        convertToAwsInstanceServiceBillingData(result);
    Map<String, VMInstanceBillingData> vmInstanceBillingDataMap = new HashMap<>();
    vmInstanceServiceBillingDataList.forEach(vmInstanceServiceBillingData -> {
      String resourceId = vmInstanceServiceBillingData.getResourceId();
      VMInstanceBillingData vmInstanceBillingData = VMInstanceBillingData.builder().resourceId(resourceId).build();
      double cost = vmInstanceServiceBillingData.getCost();
      if (vmInstanceBillingDataMap.containsKey(resourceId)) {
        vmInstanceBillingData = vmInstanceBillingDataMap.get(resourceId);
      }
      if (null != vmInstanceServiceBillingData.getUsageType()) {
        if (vmInstanceServiceBillingData.getUsageType().contains(eksNetworkInstanceType)) {
          vmInstanceBillingData = vmInstanceBillingData.toBuilder().networkCost(cost).build();
        }
        if (vmInstanceServiceBillingData.getUsageType().contains(eksCpuInstanceType)) {
          double memoryCost = vmInstanceBillingData.getMemoryCost();
          vmInstanceBillingData =
              vmInstanceBillingData.toBuilder().computeCost(memoryCost + cost).cpuCost(cost).build();
        }
        if (vmInstanceServiceBillingData.getUsageType().contains(eksMemoryInstanceType)) {
          double cpuCost = vmInstanceBillingData.getCpuCost();
          vmInstanceBillingData =
              vmInstanceBillingData.toBuilder().computeCost(cpuCost + cost).memoryCost(cost).build();
        }
      }
      vmInstanceBillingDataMap.put(resourceId, vmInstanceBillingData);
    });

    log.info("EKS Fargate resource map data {} ", vmInstanceBillingDataMap);
    return vmInstanceBillingDataMap;
  }

  private String fetchStringValue(FieldValueList row, Field field) {
    Object value = row.get(field.getName()).getValue();
    if (value != null) {
      return value.toString();
    }
    return null;
  }

  private double getNumericValue(FieldValueList row, Field field) {
    FieldValue value = row.get(field.getName());
    if (!value.isNull()) {
      return value.getNumericValue().doubleValue();
    }
    return 0;
  }

  private Double getDoubleValue(FieldValueList row, Field field) {
    FieldValue value = row.get(field.getName());
    if (!value.isNull()) {
      return value.getNumericValue().doubleValue();
    }
    return null;
  }

  public BigQuery getBigQueryService() {
    ServiceAccountCredentials credentials = getCredentials(GOOGLE_CREDENTIALS_PATH);
    return BigQueryOptions.newBuilder().setCredentials(credentials).build().getService();
  }
}
