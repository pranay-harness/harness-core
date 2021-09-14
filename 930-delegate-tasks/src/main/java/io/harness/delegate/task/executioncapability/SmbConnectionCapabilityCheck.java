/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.executioncapability;

import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.CapabilitySubjectPermissionBuilder;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.capability.SmbConnectionParameters;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SmbConnectionCapability;

import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.connection.Connection;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SmbConnectionCapabilityCheck implements CapabilityCheck, ProtoCapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    SmbConnectionCapability capability = (SmbConnectionCapability) delegateCapability;
    try {
      boolean validated = isConnectibleSOBServer(capability.getSmbUrl());
      return CapabilityResponse.builder().validated(validated).delegateCapability(capability).build();
    } catch (Exception exception) {
      log.error("Cannot Connect to SMB xxxxxxxx {}, Reason: {}", capability.getSmbUrl(), exception.getMessage());
      return CapabilityResponse.builder().validated(false).delegateCapability(capability).build();
    }
  }

  @Override
  public CapabilitySubjectPermission performCapabilityCheckWithProto(CapabilityParameters parameters) {
    CapabilitySubjectPermissionBuilder builder = CapabilitySubjectPermission.builder();
    if (parameters.getCapabilityCase() != CapabilityParameters.CapabilityCase.SMB_CONNECTION_PARAMETERS) {
      return builder.permissionResult(PermissionResult.DENIED).build();
    }
    SmbConnectionParameters smbCapability = parameters.getSmbConnectionParameters();
    try {
      boolean validated = isConnectibleSOBServer(smbCapability.getSmbUrl());
      return builder.permissionResult(validated ? PermissionResult.ALLOWED : PermissionResult.DENIED).build();
    } catch (Exception exception) {
      return builder.permissionResult(PermissionResult.DENIED).build();
    }
  }

  private String getSMBConnectionHost(String smbUrl) {
    String smbHost = smbUrl;
    if (smbHost.contains("/")) {
      smbHost = smbHost.replaceFirst("^(smb?://)", "").split("/")[0];
    } else if (smbHost.contains("\\")) {
      String[] split = smbHost.replaceFirst("^(smb?:\\\\)", "").split(Pattern.quote("\\"));
      smbHost = split[1];
    }
    return smbHost;
  }

  private boolean isConnectibleSOBServer(String smbUrl) {
    try (SMBClient client = new SMBClient(getSMBConnectionConfig());
         Connection connection = client.connect(getSMBConnectionHost(smbUrl))) {
      return true;

    } catch (Exception ex) {
      log.warn("SMB server {} could not be reached. Exception Message {}", smbUrl, ex.getMessage());
    }
    return false;
  }

  private SmbConfig getSMBConnectionConfig() {
    return SmbConfig.builder()
        .withTimeout(120, TimeUnit.SECONDS) // Timeout sets Read, Write, and Transact timeouts (default is 60 seconds)
        .withSoTimeout(180, TimeUnit.SECONDS) // Socket Timeout (default is 0 seconds, blocks forever)
        .build();
  }
}
