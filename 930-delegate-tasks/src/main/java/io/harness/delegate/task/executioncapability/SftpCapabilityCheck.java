/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.executioncapability;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.CapabilitySubjectPermissionBuilder;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SftpCapability;

import java.io.IOException;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.DisconnectReason;
import net.schmizz.sshj.transport.TransportException;

@Slf4j
public class SftpCapabilityCheck implements CapabilityCheck, ProtoCapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    SftpCapability sftpCapability = (SftpCapability) delegateCapability;
    String connectionHost = getSFTPConnectionHost(sftpCapability.getSftpUrl());
    return CapabilityResponse.builder()
        .delegateCapability(sftpCapability)
        .validated(isConnectibleSFTPServer(connectionHost))
        .build();
  }

  @Override
  public CapabilitySubjectPermission performCapabilityCheckWithProto(CapabilityParameters parameters) {
    CapabilitySubjectPermissionBuilder builder = CapabilitySubjectPermission.builder();
    if (parameters.getCapabilityCase() != CapabilityParameters.CapabilityCase.SFTP_CAPABILITY_PARAMETERS) {
      return builder.permissionResult(PermissionResult.DENIED).build();
    }
    String connectionHost = getSFTPConnectionHost(parameters.getSftpCapabilityParameters().getSftpUrl());
    return builder
        .permissionResult(isConnectibleSFTPServer(connectionHost) ? PermissionResult.ALLOWED : PermissionResult.DENIED)
        .build();
  }

  private String getSFTPConnectionHost(String sftpUrl) {
    String sftpHost = sftpUrl;
    // Check for server prefix and unix and windows style URI
    if (sftpHost.contains("/")) {
      if (sftpHost.startsWith("sftp")) {
        sftpHost = sftpHost.replaceFirst("^(sftp?://)", "").split(Pattern.quote("/"))[0];
      } else if (sftpHost.startsWith("ftp")) {
        sftpHost = sftpHost.replaceFirst("^(ftp?://)", "").split(Pattern.quote("/"))[0];
      }
    } else if (sftpHost.contains("\\")) {
      if (sftpHost.startsWith("sftp")) {
        sftpHost = sftpHost.replaceFirst("^(sftp?:\\\\)", "").split(Pattern.quote("\\"))[1];
      } else if (sftpHost.startsWith("ftp")) {
        sftpHost = sftpHost.replaceFirst("^(ftp?:\\\\)", "").split(Pattern.quote("\\"))[1];
      }
    }
    return sftpHost;
  }

  private boolean isConnectibleSFTPServer(String sftpUrl) {
    String hostKeyVerifier = "";
    try (SSHClient ssh = new SSHClient(new DefaultConfig())) {
      try {
        ssh.loadKnownHosts();
        ssh.connect(getSFTPConnectionHost(sftpUrl));
        return true;
      } catch (TransportException e) {
        if (e.getDisconnectReason() == DisconnectReason.HOST_KEY_NOT_VERIFIABLE) {
          String msg = e.getMessage();
          String[] split = msg.split("`");
          hostKeyVerifier = split[3];
        }
      }
    } catch (IOException e) {
      log.error("SFTP server {} could not be reached. Exception Message {}", sftpUrl, e.getMessage());
    }

    if (isEmpty(hostKeyVerifier)) {
      log.error("SFTP server {} host key could not be verified.", sftpUrl);
      return false;
    }

    // Try connecting again with host key verifier
    try (SSHClient ssh = new SSHClient(new DefaultConfig())) {
      ssh.addHostKeyVerifier(hostKeyVerifier);
      ssh.connect(getSFTPConnectionHost(sftpUrl));
      return true;
    } catch (IOException e) {
      log.error("SFTP server {} could not be reached. Exception Message {}", sftpUrl, e.getMessage());
    }
    return false;
  }
}
