/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.capability.internal;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.capability.CapabilityParameters;

import java.time.Duration;

@OwnedBy(HarnessTeam.DEL)
public class CapabilityAttributes {
  public static Duration getValidityPeriod(CapabilityParameters parameters) {
    switch (parameters.getCapabilityCase()) {
      case AWS_REGION_PARAMETERS:
        return Duration.ofHours(6);
      case CHART_MUSEUM_PARAMETERS:
        return Duration.ofHours(6);
      case GIT_INSTALLATION_PARAMETERS:
        return Duration.ofHours(6);
      case HELM_INSTALLATION_PARAMETERS:
        return Duration.ofHours(6);
      case HTTP_CONNECTION_PARAMETERS:
        return Duration.ofHours(6);
      case PCF_AUTO_SCALAR_PARAMETERS:
        return Duration.ofHours(6);
      case PCF_CONNECTIVITY_PARAMETERS:
        return Duration.ofHours(6);
      case PCF_INSTALLATION_PARAMETERS:
        return Duration.ofHours(6);
      case KUSTOMIZE_PARAMETERS:
        return Duration.ofHours(6);
      case PROCESS_EXECUTOR_PARAMETERS:
        return Duration.ofHours(6);
      case SFTP_CAPABILITY_PARAMETERS:
        return Duration.ofHours(6);
      case SMB_CONNECTION_PARAMETERS:
        return Duration.ofHours(6);
      case SOCKET_CONNECTIVITY_PARAMETERS:
        return Duration.ofHours(6);
      case SYSTEM_ENV_PARAMETERS:
        return Duration.ofHours(6);
      case SMTP_PARAMETERS:
        return Duration.ofHours(6);
      default:
        throw new UnsupportedOperationException("capability not supported yet, please add ValidityPeriod");
    }
  }

  public static Duration getPeriodUntilNextValidation(CapabilityParameters parameters) {
    switch (parameters.getCapabilityCase()) {
      case AWS_REGION_PARAMETERS:
        return Duration.ofHours(4);
      case CHART_MUSEUM_PARAMETERS:
        return Duration.ofHours(4);
      case GIT_INSTALLATION_PARAMETERS:
        return Duration.ofHours(4);
      case HELM_INSTALLATION_PARAMETERS:
        return Duration.ofHours(4);
      case HTTP_CONNECTION_PARAMETERS:
        return Duration.ofHours(4);
      case PCF_AUTO_SCALAR_PARAMETERS:
        return Duration.ofHours(4);
      case PCF_CONNECTIVITY_PARAMETERS:
        return Duration.ofHours(4);
      case PCF_INSTALLATION_PARAMETERS:
        return Duration.ofHours(4);
      case KUSTOMIZE_PARAMETERS:
        return Duration.ofHours(4);
      case PROCESS_EXECUTOR_PARAMETERS:
        return Duration.ofHours(4);
      case SFTP_CAPABILITY_PARAMETERS:
        return Duration.ofHours(4);
      case SMB_CONNECTION_PARAMETERS:
        return Duration.ofHours(4);
      case SOCKET_CONNECTIVITY_PARAMETERS:
        return Duration.ofHours(4);
      case SYSTEM_ENV_PARAMETERS:
        return Duration.ofHours(4);
      case SMTP_PARAMETERS:
        return Duration.ofHours(4);
      default:
        throw new UnsupportedOperationException("capability not supported yet, please add PeriondUntilNextValidation");
    }
  }

  public static String getCapabilityDescriptor(CapabilityParameters parameters) {
    switch (parameters.getCapabilityCase()) {
      case AWS_REGION_PARAMETERS:
        return "Checking that AWS is in " + parameters.getAwsRegionParameters().getRegion() + " region";
      case CHART_MUSEUM_PARAMETERS:
        return "Checking that chart museum is installed";
      case GIT_INSTALLATION_PARAMETERS:
        return "Checking that git is installed";
      case HELM_INSTALLATION_PARAMETERS:
        return "Checking that helm is installed";
      case HTTP_CONNECTION_PARAMETERS:
        return "Checking that HTTP resource " + parameters.getHttpConnectionParameters().getUrl() + " is reachable";
      case PCF_AUTO_SCALAR_PARAMETERS:
        return "Checking that PCF Autoscalar is installed";
      case PCF_CONNECTIVITY_PARAMETERS:
        return "Checking that PCF resource " + parameters.getPcfConnectivityParameters().getEndpointUrl()
            + " is reachable";
      case PCF_INSTALLATION_PARAMETERS:
        return "Checking that CF CLI is installed";
      case KUSTOMIZE_PARAMETERS:
        return "Checking that kustomize is installed at " + parameters.getKustomizeParameters().getPluginRootDir();
      case PROCESS_EXECUTOR_PARAMETERS:
        return "Checking that the following process can be executed: `"
            + String.join(" ", parameters.getProcessExecutorParameters().getArgsList()) + "`";
      case SFTP_CAPABILITY_PARAMETERS:
        return "Checking that SFTP resource " + parameters.getSftpCapabilityParameters().getSftpUrl() + " is reachable";
      case SMB_CONNECTION_PARAMETERS:
        return "Checking that SMB resource " + parameters.getSmbConnectionParameters().getSmbUrl() + " is reachable";
      case SOCKET_CONNECTIVITY_PARAMETERS:
        return "Checking that the socket " + getSocketConnectivityUrl(parameters) + " is reachable";
      case SYSTEM_ENV_PARAMETERS:
        return "Checking that on the system property `" + parameters.getSystemEnvParameters().getProperty()
            + "` equals `" + parameters.getSystemEnvParameters().getComparate() + "`";
      case SMTP_PARAMETERS:
        return "Checking that SMTP resource " + parameters.getSmtpParameters().getHost() + " is reachable";
      default:
        throw new UnsupportedOperationException("capability not supported yet, please add descriptor");
    }
  }

  private static String getSocketConnectivityUrl(CapabilityParameters parameters) {
    if (parameters.getSocketConnectivityParameters().getHostName().isEmpty()) {
      return parameters.getSocketConnectivityParameters().getUrl();
    } else {
      return parameters.getSocketConnectivityParameters().getHostName() + ":"
          + parameters.getSocketConnectivityParameters().getPort();
    }
  }
}
