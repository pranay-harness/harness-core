/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.executioncapability;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.capability.CapabilityParameters;
import io.harness.exception.GeneralException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.DEL)
public class ProtoCapabilityCheckFactory {
  @Inject AwsRegionCapabilityCheck awsRegionCapabilityCheck;
  @Inject ChartMuseumCapabilityCheck chartMuseumCapabilityCheck;
  @Inject GitInstallationCapabilityCheck gitInstallationCapabilityCheck;
  @Inject HelmInstallationCapabilityCheck helmInstallationCapabilityCheck;
  @Inject HttpConnectionExecutionCapabilityCheck httpConnectionExecutionCapabilityCheck;
  @Inject PcfAutoScalarCapabilityCheck pcfAutoScalarCapabilityCheck;
  @Inject KustomizeCapabilityCheck kustomizeCapabilityCheck;
  @Inject PcfConnectivityCapabilityCheck pcfConnectivityCapabilityCheck;
  @Inject PcfInstallationCapabilityCheck pcfInstallationCapabilityCheck;
  @Inject ProcessExecutorCapabilityCheck processExecutorCapabilityCheck;
  @Inject SftpCapabilityCheck sftpCapabilityCheck;
  @Inject SmbConnectionCapabilityCheck smbConnectionCapabilityCheck;
  @Inject SmtpCapabilityCheck smtpCapabilityCheck;
  @Inject SocketConnectivityCapabilityCheck socketConnectivityCapabilityCheck;
  @Inject SystemEnvCapabilityCheck systemEnvCapabilityCheck;

  public ProtoCapabilityCheck obtainCapabilityCheck(CapabilityParameters parameters) {
    if (parameters == null) {
      return null;
    }
    switch (parameters.getCapabilityCase()) {
      case AWS_REGION_PARAMETERS:
        return awsRegionCapabilityCheck;
      case CHART_MUSEUM_PARAMETERS:
        return chartMuseumCapabilityCheck;
      case GIT_INSTALLATION_PARAMETERS:
        return gitInstallationCapabilityCheck;
      case HELM_INSTALLATION_PARAMETERS:
        return helmInstallationCapabilityCheck;
      case HTTP_CONNECTION_PARAMETERS:
        return httpConnectionExecutionCapabilityCheck;
      case KUSTOMIZE_PARAMETERS:
        return kustomizeCapabilityCheck;
      case PCF_AUTO_SCALAR_PARAMETERS:
        return pcfAutoScalarCapabilityCheck;
      case PCF_CONNECTIVITY_PARAMETERS:
        return pcfConnectivityCapabilityCheck;
      case PCF_INSTALLATION_PARAMETERS:
        return pcfInstallationCapabilityCheck;
      case PROCESS_EXECUTOR_PARAMETERS:
        return processExecutorCapabilityCheck;
      case SFTP_CAPABILITY_PARAMETERS:
        return sftpCapabilityCheck;
      case SMB_CONNECTION_PARAMETERS:
        return smbConnectionCapabilityCheck;
      case SMTP_PARAMETERS:
        return smtpCapabilityCheck;
      case SOCKET_CONNECTIVITY_PARAMETERS:
        return socketConnectivityCapabilityCheck;
      case SYSTEM_ENV_PARAMETERS:
        return systemEnvCapabilityCheck;
      default:
        throw new GeneralException("capability is not registered");
    }
  }
}
