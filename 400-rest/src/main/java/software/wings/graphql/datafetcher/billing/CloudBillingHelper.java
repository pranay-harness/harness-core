/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.ccm.billing.graphql.CloudEntityGroupBy.cloudProvider;
import static io.harness.ccm.billing.graphql.CloudEntityGroupBy.labelsKey;
import static io.harness.ccm.billing.graphql.CloudEntityGroupBy.labelsValue;
import static io.harness.ccm.billing.graphql.CloudEntityGroupBy.tagsKey;
import static io.harness.ccm.billing.graphql.CloudEntityGroupBy.tagsValue;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.billing.dao.BillingDataPipelineRecordDao;
import io.harness.ccm.billing.graphql.CloudBillingAggregate;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudBillingGroupBy;
import io.harness.ccm.billing.graphql.CloudBillingIdFilter;
import io.harness.ccm.billing.preaggregated.PreAggregateConstants;
import io.harness.ccm.commons.entities.billing.BillingDataPipelineRecord;
import io.harness.ccm.setup.config.CESetUpConfig;
import io.harness.exception.InvalidRequestException;

import software.wings.app.MainConfiguration;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.CustomSql;
import com.healthmarketscience.sqlbuilder.SqlObject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

@Singleton
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class CloudBillingHelper {
  @Inject private MainConfiguration mainConfiguration;
  @Inject private BillingDataPipelineRecordDao billingDataPipelineRecordDao;

  public static final String unified = "unifiedTable";
  private static final String preAggregated = "preAggregated";
  private static final String awsRawTable = "awscur*";
  private static final String gcpRawTable = "gcp_billing_export*";
  private static final String azureRawTable = "azureBilling_%s_%s";
  public static final String informationSchema = "INFORMATION_SCHEMA";
  private static final String leftJoinTemplate = " LEFT JOIN UNNEST(%s) as %s";
  private static final String tags = "tags";
  private static final String labels = "labels";
  private static final String credits = "credits";
  public static final String DATA_SET_NAME_TEMPLATE = "BillingReport_%s";
  public static final String columnView = "COLUMNS";

  private Cache<String, BillingDataPipelineCacheObject> billingDataPipelineRecordCache =
      Caffeine.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build();

  public BillingDataPipelineCacheObject getDataPipelineMetadata(String accountId) {
    return billingDataPipelineRecordCache.get(accountId, key -> getBillingDataPipelineCacheObject(key));
  }

  public BillingDataPipelineCacheObject getBillingDataPipelineCacheObject(String accountId) {
    List<BillingDataPipelineRecord> listOfPipelineRecords =
        billingDataPipelineRecordDao.fetchBillingPipelineRecords(accountId);
    if (listOfPipelineRecords.isEmpty()) {
      return BillingDataPipelineCacheObject.builder().build();
    } else {
      BillingDataPipelineRecord billingDataPipelineRecord = getPipelineRecord(listOfPipelineRecords);
      return BillingDataPipelineCacheObject.builder()
          .dataSetId(billingDataPipelineRecord.getDataSetId())
          .awsLinkedAccountsToExclude(billingDataPipelineRecord.getAwsLinkedAccountsToExclude())
          .awsLinkedAccountsToInclude(billingDataPipelineRecord.getAwsLinkedAccountsToInclude())
          .build();
    }
  }

  private BillingDataPipelineRecord getPipelineRecord(List<BillingDataPipelineRecord> listOfPipelineRecords) {
    return listOfPipelineRecords.stream()
        .filter(billingDataPipelineRecord -> billingDataPipelineRecord.getCloudProvider().equals("AWS"))
        .findFirst()
        .orElse(listOfPipelineRecords.get(0));
  }

  public String getCloudProviderTableName(String accountId) {
    return getCloudProviderTableName(accountId, preAggregated);
  }

  public String getCloudProviderTableName(String accountId, String tableName) {
    CESetUpConfig ceSetUpConfig = mainConfiguration.getCeSetUpConfig();
    String projectId = ceSetUpConfig.getGcpProjectId();
    String dataSetId = getDataSetId(accountId);
    return format("%s.%s.%s", projectId, dataSetId, tableName);
  }

  public String getInformationSchemaViewForDataset(String accountId, String view) {
    CESetUpConfig ceSetUpConfig = mainConfiguration.getCeSetUpConfig();
    String projectId = ceSetUpConfig.getGcpProjectId();
    String dataSetId = getDataSetId(accountId);
    return format("%s.%s.%s.%s", projectId, dataSetId, informationSchema, view);
  }

  public String getCloudProviderTableName(String gcpProjectId, String accountId, String tableName) {
    String dataSetId = getDataSetId(accountId);
    return format("%s.%s.%s", gcpProjectId, dataSetId, tableName);
  }

  public String getDataSetId(String accountId) {
    return String.format(DATA_SET_NAME_TEMPLATE, modifyStringToComplyRegex(accountId));
  }

  public String modifyStringToComplyRegex(String accountInfo) {
    return accountInfo.toLowerCase().replaceAll("[^a-z0-9]", "_");
  }

  public String getCloudProvider(List<CloudBillingFilter> filters) {
    Optional<CloudBillingFilter> cloudProviderFilter =
        filters.stream().filter(billingFilter -> billingFilter.getCloudProvider() != null).findFirst();

    if (cloudProviderFilter.isPresent()) {
      return (String) cloudProviderFilter.get().getCloudProvider().getValues()[0];
    } else {
      throw new InvalidRequestException("Cloud Provider cannot be null");
    }
  }

  public String getTableName(String cloudProvider) {
    switch (cloudProvider) {
      case "AWS":
        return awsRawTable;
      case "GCP":
        return gcpRawTable;
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
    return format(azureRawTable, dateElements[0], dateElements[1]);
  }

  public SqlObject getLeftJoin(String cloudProvider) {
    switch (cloudProvider) {
      case "AWS":
        return new CustomSql(String.format(leftJoinTemplate, tags, tags));
      case "GCP":
        return new CustomSql(String.format(leftJoinTemplate, labels, labels));
      default:
        throw new InvalidRequestException("Invalid Cloud Provider");
    }
  }

  public List<CloudBillingFilter> removeAndReturnCloudProviderFilter(List<CloudBillingFilter> filters) {
    return filters.stream()
        .filter(billingFilter -> billingFilter.getCloudProvider() == null)
        .collect(Collectors.toList());
  }

  public List<CloudBillingGroupBy> removeAndReturnCloudProviderGroupBy(List<CloudBillingGroupBy> groupByList) {
    return groupByList.stream()
        .filter(billingGroupBy -> billingGroupBy.getEntityGroupBy() != cloudProvider)
        .collect(Collectors.toList());
  }

  public Boolean fetchIfRawTableQueryRequired(List<CloudBillingFilter> filters, List<CloudBillingGroupBy> groupByList) {
    boolean labelsFilterPresent = filters.stream().anyMatch(billingFilter
        -> billingFilter.getLabelsValue() != null || billingFilter.getLabelsKey() != null
            || billingFilter.getLabels() != null || billingFilter.getTagsValue() != null
            || billingFilter.getTagsKey() != null || billingFilter.getTags() != null);

    boolean labelsGroupByPresent = groupByList.stream().anyMatch(billingGroupBy
        -> billingGroupBy.getEntityGroupBy() == labelsKey || billingGroupBy.getEntityGroupBy() == labelsValue
            || billingGroupBy.getEntityGroupBy() == tagsKey || billingGroupBy.getEntityGroupBy() == tagsValue);

    return labelsFilterPresent || labelsGroupByPresent;
  }

  public Boolean fetchIfDiscountsAggregationPresent(List<CloudBillingAggregate> aggregateFunction) {
    return aggregateFunction.stream().anyMatch(
        aggregation -> aggregation.getColumnName().equals(CloudBillingAggregate.BILLING_GCP_CREDITS));
  }

  @NotNull
  public Function<CloudBillingFilter, Condition> getFiltersMapper(
      boolean isAWSCloudProvider, boolean isQueryRawTableRequired) {
    if (isQueryRawTableRequired) {
      return isAWSCloudProvider ? CloudBillingFilter::toAwsRawTableCondition : CloudBillingFilter::toRawTableCondition;
    } else {
      return CloudBillingFilter::toCondition;
    }
  }

  @NotNull
  protected Function<CloudBillingGroupBy, Object> getGroupByMapper(
      boolean isAWSCloudProvider, boolean isQueryRawTableRequired) {
    if (isQueryRawTableRequired) {
      return isAWSCloudProvider ? CloudBillingGroupBy::toAwsRawTableGroupbyObject
                                : CloudBillingGroupBy::toRawTableGroupbyObject;
    } else {
      return CloudBillingGroupBy::toGroupbyObject;
    }
  }

  @NotNull
  protected Function<CloudBillingAggregate, SqlObject> getAggregationMapper(
      boolean isAWSCloudProvider, boolean isQueryRawTableRequired) {
    if (isQueryRawTableRequired) {
      return isAWSCloudProvider ? CloudBillingAggregate::toAwsRawTableFunctionCall
                                : CloudBillingAggregate::toRawTableFunctionCall;

    } else {
      return CloudBillingAggregate::toFunctionCall;
    }
  }

  public SqlObject getCreditsLeftJoin() {
    return new CustomSql(String.format(leftJoinTemplate, credits, credits));
  }

  public void processAndAddLinkedAccountsFilter(String accountId, List<CloudBillingFilter> filters) {
    String[] linkedAccountsToBlacklist = getLinkedAccountsToBlacklist(accountId);
    String[] linkedAccountsToWhitelist = getLinkedAccountsToWhitelist(accountId);

    boolean isLinkedAccountFilterPresent =
        filters.stream().anyMatch(billingFilter -> billingFilter.getAwsLinkedAccount() != null);

    if (linkedAccountsToBlacklist != null) {
      CloudBillingFilter cloudBillingFilter = new CloudBillingFilter();
      cloudBillingFilter.setAwsLinkedAccount(
          CloudBillingIdFilter.builder().operator(QLIdOperator.NOT_IN).values(linkedAccountsToBlacklist).build());
      filters.add(cloudBillingFilter);
    }

    if (!isLinkedAccountFilterPresent && linkedAccountsToWhitelist != null) {
      CloudBillingFilter cloudBillingFilter = new CloudBillingFilter();
      cloudBillingFilter.setAwsLinkedAccount(
          CloudBillingIdFilter.builder().operator(QLIdOperator.IN).values(linkedAccountsToWhitelist).build());
      filters.add(cloudBillingFilter);
    }
  }

  private String[] getLinkedAccountsToBlacklist(String accountId) {
    BillingDataPipelineCacheObject dataPipelineMetadata = getDataPipelineMetadata(accountId);
    List<String> awsLinkedAccountsToExclude = dataPipelineMetadata.getAwsLinkedAccountsToExclude();

    if (awsLinkedAccountsToExclude != null && awsLinkedAccountsToExclude.size() > 0) {
      List<String> awsLinkedAccounts = new ArrayList<>(awsLinkedAccountsToExclude);
      awsLinkedAccounts.add(PreAggregateConstants.entityConstantAwsNoLinkedAccount);
      String[] linkedAccounts = new String[awsLinkedAccounts.size()];
      return awsLinkedAccounts.toArray(linkedAccounts);
    }
    return null;
  }

  private String[] getLinkedAccountsToWhitelist(String accountId) {
    BillingDataPipelineCacheObject dataPipelineMetadata = getDataPipelineMetadata(accountId);
    List<String> awsLinkedAccountsToInclude = dataPipelineMetadata.getAwsLinkedAccountsToInclude();

    if (awsLinkedAccountsToInclude != null && awsLinkedAccountsToInclude.size() > 0) {
      List<String> awsLinkedAccounts = new ArrayList<>(awsLinkedAccountsToInclude);
      awsLinkedAccounts.add(PreAggregateConstants.entityConstantAwsNoLinkedAccount);
      String[] linkedAccounts = new String[awsLinkedAccounts.size()];
      return awsLinkedAccounts.toArray(linkedAccounts);
    }
    return null;
  }
}
