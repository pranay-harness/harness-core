package io.harness.batch.processing.pricing.gcp.bigquery.impl;

import static io.harness.batch.processing.pricing.gcp.bigquery.BigQueryConstants.computeProductFamily;
import static io.harness.batch.processing.pricing.gcp.bigquery.BigQueryConstants.cost;
import static io.harness.batch.processing.pricing.gcp.bigquery.BigQueryConstants.effectiveCost;
import static io.harness.batch.processing.pricing.gcp.bigquery.BigQueryConstants.networkProductFamily;
import static io.harness.batch.processing.pricing.gcp.bigquery.BigQueryConstants.productFamily;
import static io.harness.batch.processing.pricing.gcp.bigquery.BigQueryConstants.resourceId;
import static io.harness.batch.processing.pricing.gcp.bigquery.BigQueryConstants.serviceCode;
import static io.harness.ccm.billing.GcpServiceAccountServiceImpl.getCredentials;

import static java.lang.String.format;

import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.config.BillingDataPipelineConfig;
import io.harness.batch.processing.pricing.data.VMInstanceBillingData;
import io.harness.batch.processing.pricing.data.VMInstanceServiceBillingData;
import io.harness.batch.processing.pricing.data.VMInstanceServiceBillingData.VMInstanceServiceBillingDataBuilder;
import io.harness.batch.processing.pricing.gcp.bigquery.BigQueryConstants;
import io.harness.batch.processing.pricing.gcp.bigquery.BigQueryHelperService;

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

  @Autowired
  public BigQueryHelperServiceImpl(BatchMainConfig mainConfig) {
    this.mainConfig = mainConfig;
  }

  private static final String GOOGLE_CREDENTIALS_PATH = "GOOGLE_CREDENTIALS_PATH";
  private static final String AWS_CUR_TABLE_NAME = "awscur_%s_%s";

  @Override
  public Map<String, VMInstanceBillingData> getAwsEC2BillingData(
      List<String> resourceId, Instant startTime, Instant endTime, String dataSetId) {
    String query = BigQueryConstants.AWS_EC2_BILLING_QUERY;
    String resourceIds = String.join("','", resourceId);
    String projectTableName = getAwsProjectTableName(startTime, dataSetId);
    String formattedQuery = format(query, projectTableName, resourceIds, startTime, endTime);
    return query(formattedQuery);
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
      log.error("Failed to get PreAggregateBillingOverview. {}", e);
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
            dataBuilder.resourceId(fetchStringValue(row, field));
            break;
          case serviceCode:
            dataBuilder.serviceCode(fetchStringValue(row, field));
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
