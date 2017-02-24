package software.wings.service.impl;

import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.utils.SshHelperUtil.normalizeError;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import software.wings.beans.ErrorCode;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.HostValidationResponse;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.infrastructure.Host;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.core.ssh.executors.SshSessionFactory;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureProvider;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.SshHelperUtil;

/**
 * Created by anubhaw on 1/12/17.
 */
@Singleton
public class StaticInfrastructureProvider implements InfrastructureProvider {
  @Inject private HostService hostService;

  @Override
  public PageResponse<Host> listHosts(SettingAttribute computeProviderSetting, PageRequest<Host> req) {
    return hostService.list(req);
  }

  @Override
  public Host saveHost(Host host) {
    return hostService.saveHost(host);
  }

  @Override
  public void deleteHost(String appId, String infraMappingId, String hostName) {
    hostService.deleteByHostName(appId, infraMappingId, hostName);
  }

  @Override
  public void updateHostConnAttrs(InfrastructureMapping infrastructureMapping, String hostConnectionAttrs) {
    hostService.updateHostConnectionAttrByInfraMappingId(
        infrastructureMapping.getAppId(), infrastructureMapping.getUuid(), hostConnectionAttrs);
  }

  @Override
  public void deleteHostByInfraMappingId(String appId, String infraMappingId) {
    hostService.deleteByInfraMappingId(appId, infraMappingId);
  }

  public HostValidationResponse validateHost(
      String hostName, SettingAttribute connectionSetting, ExecutionCredential executionCredential) {
    CommandExecutionContext commandExecutionContext = aCommandExecutionContext()
                                                          .withHostConnectionAttributes(connectionSetting)
                                                          .withExecutionCredential(executionCredential)
                                                          .build();
    SshSessionConfig sshSessionConfig =
        SshHelperUtil.getSshSessionConfig(hostName, "HOST_CONNECTION_TEST", commandExecutionContext);
    HostValidationResponse response = HostValidationResponse.Builder.aHostValidationResponse()
                                          .withHostName(hostName)
                                          .withStatus(ExecutionStatus.SUCCESS.name())
                                          .build();
    try {
      Session sshSession = SshSessionFactory.getSSHSession(sshSessionConfig);
      sshSession.disconnect();
    } catch (JSchException jschEx) {
      ErrorCode errorCode = normalizeError(jschEx);
      response.setStatus(ExecutionStatus.FAILED.name());
      response.setErrorCode(errorCode.getCode());
      response.setErrorDescription(errorCode.getDescription());
    }
    return response;
  }
}
