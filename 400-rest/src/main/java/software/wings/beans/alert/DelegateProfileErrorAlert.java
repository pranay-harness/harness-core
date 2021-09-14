/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.alert;

import static java.lang.String.format;

import io.harness.alert.AlertData;
import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.k8s.KubernetesConvention;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
@TargetModule(HarnessModule._920_DELEGATE_SERVICE_BEANS)
@BreakDependencyOn("io.harness.k8s.KubernetesConvention")
public class DelegateProfileErrorAlert implements AlertData {
  private String obfuscatedIpAddress;
  private String hostName;
  private String accountId;

  @Override
  public boolean matches(AlertData alertData) {
    DelegateProfileErrorAlert delegateProfileErrorAlert = (DelegateProfileErrorAlert) alertData;
    return StringUtils.equals(accountId, delegateProfileErrorAlert.getAccountId())
        && StringUtils.equals(hostName, delegateProfileErrorAlert.getHostName())
        && (hostName.contains(KubernetesConvention.getAccountIdentifier(accountId))
            || StringUtils.equals(obfuscatedIpAddress, delegateProfileErrorAlert.getObfuscatedIpAddress()));
  }

  @Override
  public String buildTitle() {
    return format("Delegate %s has a profile error", hostName);
  }
}
