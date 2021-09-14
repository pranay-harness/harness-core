/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.helpers.ext.chartmuseum;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.chartmuseum.ChartMuseumClientHelper;
import io.harness.chartmuseum.ChartMuseumServer;
import io.harness.exception.WingsException;

import software.wings.beans.AwsConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.settings.helm.AmazonS3HelmRepoConfig;
import software.wings.beans.settings.helm.GCSHelmRepoConfig;
import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.settings.SettingValue;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.StartedProcess;

@Slf4j
@TargetModule(HarnessModule._960_API_SERVICES)
public class ChartMuseumClientImpl implements ChartMuseumClient {
  @Inject private ChartMuseumClientHelper chartMuseumClientHelper;

  @Override
  public ChartMuseumServer startChartMuseumServer(HelmRepoConfig helmRepoConfig, SettingValue connectorConfig,
      String resourceDirectory, String basePath) throws Exception {
    log.info("Starting chart museum server");

    if (helmRepoConfig instanceof AmazonS3HelmRepoConfig) {
      return startAmazonS3ChartMuseumServer(helmRepoConfig, connectorConfig, basePath);
    } else if (helmRepoConfig instanceof GCSHelmRepoConfig) {
      return startGCSChartMuseumServer(helmRepoConfig, connectorConfig, resourceDirectory, basePath);
    }

    throw new WingsException("Unhandled type of helm repo config. Type : " + helmRepoConfig.getSettingType());
  }

  private ChartMuseumServer startGCSChartMuseumServer(HelmRepoConfig helmRepoConfig, SettingValue connectorConfig,
      String resourceDirectory, String basePath) throws Exception {
    GCSHelmRepoConfig gcsHelmRepoConfig = (GCSHelmRepoConfig) helmRepoConfig;
    GcpConfig config = (GcpConfig) connectorConfig;

    return chartMuseumClientHelper.startGCSChartMuseumServer(
        gcsHelmRepoConfig.getBucketName(), basePath, config.getServiceAccountKeyFileContent(), resourceDirectory);
  }

  private ChartMuseumServer startAmazonS3ChartMuseumServer(
      HelmRepoConfig helmRepoConfig, SettingValue connectorConfig, String basePath) throws Exception {
    AmazonS3HelmRepoConfig amazonS3HelmRepoConfig = (AmazonS3HelmRepoConfig) helmRepoConfig;
    AwsConfig awsConfig = (AwsConfig) connectorConfig;

    return chartMuseumClientHelper.startS3ChartMuseumServer(amazonS3HelmRepoConfig.getBucketName(), basePath,
        amazonS3HelmRepoConfig.getRegion(), awsConfig.isUseEc2IamCredentials(), awsConfig.getAccessKey(),
        awsConfig.getSecretKey(), awsConfig.isUseIRSA());
  }

  @Override
  public void stopChartMuseumServer(StartedProcess process) {
    chartMuseumClientHelper.stopChartMuseumServer(process);
  }
}
