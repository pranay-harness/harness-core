package io.harness.ccm.setup.graphql;

import static io.harness.persistence.HQuery.excludeValidate;

import io.harness.beans.FeatureName;
import io.harness.ccm.commons.dao.CEMetadataRecordDao;
import io.harness.ccm.commons.entities.CEMetadataRecord;
import io.harness.ccm.setup.graphql.QLCEOverviewStatsData.QLCEOverviewStatsDataBuilder;
import io.harness.ccm.views.service.CEViewService;
import io.harness.ff.FeatureFlagService;
import io.harness.persistence.HPersistence;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.query.QLNoOpQueryParameters;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OverviewPageStatsDataFetcher
    extends AbstractObjectDataFetcher<QLCEOverviewStatsData, QLNoOpQueryParameters> {
  @Inject HPersistence persistence;
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject protected DataFetcherUtils utils;
  @Inject protected FeatureFlagService featureFlagService;
  @Inject private CEViewService ceViewService;
  @Inject private CEMetadataRecordDao metadataRecordDao;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLCEOverviewStatsData fetch(QLNoOpQueryParameters parameters, String accountId) {
    CEMetadataRecord ceMetadataRecord = metadataRecordDao.getByAccountId(accountId);

    boolean isAWSConnectorPresent = false;
    boolean isGCPConnectorPresent = false;
    boolean isAzureConnectorPresent = false;
    boolean isApplicationDataPresent = false;
    boolean isClusterDataPresent = false;
    if (ceMetadataRecord != null) {
      if (ceMetadataRecord.getAwsConnectorConfigured() != null) {
        isAWSConnectorPresent = ceMetadataRecord.getAwsConnectorConfigured();
      }
      if (ceMetadataRecord.getGcpConnectorConfigured() != null) {
        isGCPConnectorPresent = ceMetadataRecord.getGcpConnectorConfigured();
      }
      if (ceMetadataRecord.getAzureConnectorConfigured() != null) {
        isAzureConnectorPresent = ceMetadataRecord.getAzureConnectorConfigured();
      }
      if (ceMetadataRecord.getClusterDataConfigured() != null) {
        isClusterDataPresent = ceMetadataRecord.getClusterDataConfigured();
      }
      if (ceMetadataRecord.getApplicationDataPresent() != null) {
        isApplicationDataPresent = ceMetadataRecord.getApplicationDataPresent();
      }
    }

    QLCEOverviewStatsDataBuilder overviewStatsDataBuilder = QLCEOverviewStatsData.builder();

    overviewStatsDataBuilder.cloudConnectorsPresent(
        isAWSConnectorPresent || isGCPConnectorPresent || isAzureConnectorPresent);

    boolean isCeEnabledCloudProviderPresent = getCEEnabledCloudProvider(accountId);

    if (!isClusterDataPresent
        && featureFlagService.isEnabledReloadCache(FeatureName.CE_SAMPLE_DATA_GENERATION, accountId)) {
      log.info("Sample data generation enabled for accountId:{}", accountId);
      if (utils.isSampleClusterDataPresent()) {
        isClusterDataPresent = true;
        overviewStatsDataBuilder.isSampleClusterPresent(true);
        log.info("sample data is present");
      }
    }
    overviewStatsDataBuilder.clusterDataPresent(isClusterDataPresent)
        .applicationDataPresent(isApplicationDataPresent)
        .ceEnabledClusterPresent(isCeEnabledCloudProviderPresent);

    // AWS, GCP, AZURE Data Present

    if (ceMetadataRecord != null) {
      if (ceMetadataRecord.getAwsDataPresent() != null) {
        isAWSConnectorPresent = ceMetadataRecord.getAwsDataPresent();
      } else {
        isAWSConnectorPresent = false;
      }
      if (ceMetadataRecord.getGcpDataPresent() != null) {
        isGCPConnectorPresent = ceMetadataRecord.getGcpDataPresent();
      } else {
        isGCPConnectorPresent = false;
      }
      if (ceMetadataRecord.getAzureDataPresent() != null) {
        isAzureConnectorPresent = ceMetadataRecord.getAzureDataPresent();
      } else {
        isAzureConnectorPresent = false;
      }
    }

    overviewStatsDataBuilder.awsConnectorsPresent(isAWSConnectorPresent)
        .gcpConnectorsPresent(isGCPConnectorPresent)
        .azureConnectorsPresent(isAzureConnectorPresent);

    if (overviewStatsDataBuilder.build().getAzureConnectorsPresent() != null
        && overviewStatsDataBuilder.build().getAzureConnectorsPresent()) {
      overviewStatsDataBuilder.defaultAzurePerspectiveId(ceViewService.getDefaultAzureViewId(accountId));
    }
    log.info("Returning /overviewPageStats ");
    return overviewStatsDataBuilder.build();
  }

  protected boolean getCEEnabledCloudProvider(String accountId) {
    return null
        != persistence.createQuery(SettingAttribute.class, excludeValidate)
               .field(SettingAttributeKeys.accountId)
               .equal(accountId)
               .field(SettingAttributeKeys.category)
               .equal(SettingCategory.CLOUD_PROVIDER.toString())
               .field(SettingAttributeKeys.isCEEnabled)
               .equal(true)
               .get();
  }
}
