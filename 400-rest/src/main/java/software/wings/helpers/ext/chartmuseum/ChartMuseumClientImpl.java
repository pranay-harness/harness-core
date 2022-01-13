/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
import software.wings.delegatetasks.ExceptionMessageSanitizer;
import software.wings.settings.SettingValue;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.StartedProcess;

@Slf4j
@TargetModule(HarnessModule._960_API_SERVICES)
public class ChartMuseumClientImpl implements ChartMuseumClient {
  @Inject private ChartMuseumClientHelper chartMuseumClientHelper;

  @Override
  public ChartMuseumServer startChartMuseumServer(HelmRepoConfig helmRepoConfig, SettingValue connectorConfig,
      String resourceDirectory, String basePath, boolean useLatestChartMuseumVersion) throws Exception {
    log.info("Starting chart museum server");

    if (helmRepoConfig instanceof AmazonS3HelmRepoConfig) {
      return startAmazonS3ChartMuseumServer(helmRepoConfig, connectorConfig, basePath, useLatestChartMuseumVersion);
    } else if (helmRepoConfig instanceof GCSHelmRepoConfig) {
      return startGCSChartMuseumServer(
          helmRepoConfig, connectorConfig, resourceDirectory, basePath, useLatestChartMuseumVersion);
    }

    throw new WingsException("Unhandled type of helm repo config. Type : " + helmRepoConfig.getSettingType());
  }

  private ChartMuseumServer startGCSChartMuseumServer(HelmRepoConfig helmRepoConfig, SettingValue connectorConfig,
      String resourceDirectory, String basePath, boolean useLatestChartMuseumVersion) throws Exception {
    GCSHelmRepoConfig gcsHelmRepoConfig = (GCSHelmRepoConfig) helmRepoConfig;
    GcpConfig config = (GcpConfig) connectorConfig;

    return chartMuseumClientHelper.startGCSChartMuseumServer(gcsHelmRepoConfig.getBucketName(), basePath,
        config.getServiceAccountKeyFileContent(), resourceDirectory, useLatestChartMuseumVersion);
  }

  private ChartMuseumServer startAmazonS3ChartMuseumServer(HelmRepoConfig helmRepoConfig, SettingValue connectorConfig,
      String basePath, boolean useLatestChartMuseumVersion) throws Exception {
    AmazonS3HelmRepoConfig amazonS3HelmRepoConfig = (AmazonS3HelmRepoConfig) helmRepoConfig;
    AwsConfig awsConfig = (AwsConfig) connectorConfig;

    try {
      return chartMuseumClientHelper.startS3ChartMuseumServer(amazonS3HelmRepoConfig.getBucketName(), basePath,
          amazonS3HelmRepoConfig.getRegion(), awsConfig.isUseEc2IamCredentials(), awsConfig.getAccessKey(),
          awsConfig.getSecretKey(), awsConfig.isUseIRSA(), useLatestChartMuseumVersion);
    } catch (Exception ex) {
      List<String> secrets = new ArrayList<>();
      secrets.add(String.valueOf(awsConfig.getSecretKey()));
      if (awsConfig.isUseEncryptedAccessKey()) {
        secrets.add(String.valueOf(awsConfig.getAccessKey()));
      }
      ExceptionMessageSanitizer.sanitizeException(ex, secrets);
      throw ex;
    }
  }

  @Override
  public void stopChartMuseumServer(StartedProcess process) {
    chartMuseumClientHelper.stopChartMuseumServer(process);
  }
}
