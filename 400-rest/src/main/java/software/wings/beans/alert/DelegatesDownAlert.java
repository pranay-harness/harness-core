/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
public class DelegatesDownAlert implements AlertData {
  private String obfuscatedIpAddress;
  private String hostName;
  private String accountId;

  @Override
  public boolean matches(AlertData alertData) {
    DelegatesDownAlert delegatesDownAlert = (DelegatesDownAlert) alertData;
    return StringUtils.equals(accountId, delegatesDownAlert.getAccountId())
        && StringUtils.equals(hostName, delegatesDownAlert.getHostName())
        && (hostName.contains(KubernetesConvention.getAccountIdentifier(accountId))
            || StringUtils.equals(obfuscatedIpAddress, delegatesDownAlert.getObfuscatedIpAddress()));
  }

  @Override
  public String buildTitle() {
    return format("Delegate %s is down", hostName);
  }
}
