package io.harness.ccm;

import static java.util.Objects.isNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.cluster.entities.ClusterRecord;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ValidationResult;
import software.wings.beans.ValidationResult.ValidationResultBuilder;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Singleton
public class CCMSettingServiceImpl implements CCMSettingService {
  private MainConfiguration configuration;
  private AccountService accountService;
  private SettingsService settingsService;

  // TODO: (Rohit) Update the Base path
  private static final String HARNESS_BASE_PATH = "harness_base_path";
  private static final String AWS_ACCESS_KEY_ID = "AWS_ACCESS_KEY_ID";
  private static final String AWS_SECRET_ACCESS_KEY = "AWS_SECRET_ACCESS_KEY";
  private static final String AWS_DEFAULT_REGION = "AWS_DEFAULT_REGION";

  @Inject
  public CCMSettingServiceImpl(
      AccountService accountService, SettingsService settingsService, MainConfiguration configuration) {
    this.accountService = accountService;
    this.settingsService = settingsService;
    this.configuration = configuration;
  }

  @Override
  public boolean isCloudCostEnabled(SettingAttribute settingAttribute) {
    Account account = accountService.get(settingAttribute.getAccountId());
    if (account.isCloudCostEnabled()) {
      CloudCostAware value = (CloudCostAware) settingAttribute.getValue();
      CCMConfig ccmConfig = value.getCcmConfig();
      if (null != ccmConfig) {
        return ccmConfig.isCloudCostEnabled();
      }
    }
    return false;
  }

  @Override
  public SettingAttribute maskCCMConfig(SettingAttribute settingAttribute) {
    Account account = accountService.get(settingAttribute.getAccountId());
    if (!account.isCloudCostEnabled()) {
      CloudCostAware value = (CloudCostAware) settingAttribute.getValue();
      value.setCcmConfig(null);
      settingAttribute.setValue((SettingValue) value);
    }
    return settingAttribute;
  }

  @Override
  public boolean isCloudCostEnabled(ClusterRecord clusterRecord) {
    String cloudProviderId = clusterRecord.getCluster().getCloudProviderId();
    SettingAttribute settingAttribute = settingsService.get(cloudProviderId);
    if (isNull(settingAttribute)) {
      logger.error("Failed to find the Cloud Provider associated with the Cluster with id={}", clusterRecord.getUuid());
      return false;
    }
    if (settingAttribute.getValue() instanceof CloudCostAware) {
      return isCloudCostEnabled(settingAttribute);
    }
    return false;
  }

  @Override
  public ValidationResult validateS3SyncConfig(AwsS3SyncConfig awsS3SyncConfig, String accountId, String settingId) {
    ImmutableMap<String, String> envVariables =
        ImmutableMap.<String, String>builder()
            .put(AWS_ACCESS_KEY_ID, configuration.getAwsS3SyncConfig().getAwsAccessKey())
            .put(AWS_SECRET_ACCESS_KEY, configuration.getAwsS3SyncConfig().getAwsSecretKey())
            .put(AWS_DEFAULT_REGION, configuration.getAwsS3SyncConfig().getRegion())
            .build();

    ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
    StringJoiner pathJoiner = new StringJoiner("/");
    String year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
    String month = String.valueOf(Calendar.getInstance().get(Calendar.MONTH));
    pathJoiner.add("s3://" + HARNESS_BASE_PATH).add(accountId).add(settingId).add(year).add(month);
    String destinationBucketPath = pathJoiner.toString();
    try {
      final ArrayList<String> dryRunCmd =
          Lists.newArrayList("aws", "s3", "sync", awsS3SyncConfig.getBillingBucketPath(), destinationBucketPath,
              "--source-region", awsS3SyncConfig.getBillingBucketRegion(), "--dryrun");
      ProcessResult processResult = new ProcessExecutor()
                                        .command(dryRunCmd)
                                        .timeout(1, TimeUnit.MINUTES)
                                        .environment(envVariables)
                                        .readOutput(true)
                                        .execute();
      validationResultBuilder.valid(processResult.getExitValue() == 0);
      validationResultBuilder.errorMessage(processResult.outputString());
      return validationResultBuilder.build();
    } catch (IOException | TimeoutException e) {
      logger.error("Exception during s3 sync Config Validation for src={}, srcRegion={}, dest={}",
          awsS3SyncConfig.getBillingBucketPath(), awsS3SyncConfig.getBillingBucketRegion(), destinationBucketPath);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return validationResultBuilder.valid(false).errorMessage("TimeOut Exception, Please Retry").build();
  }
}
