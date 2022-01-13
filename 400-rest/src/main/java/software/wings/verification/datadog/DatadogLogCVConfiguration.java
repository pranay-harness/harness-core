/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.verification.datadog;

import software.wings.verification.CVConfiguration;
import software.wings.verification.log.LogsCVConfiguration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DatadogLogCVConfiguration extends LogsCVConfiguration {
  private String hostnameField;

  @Override
  public CVConfiguration deepCopy() {
    DatadogLogCVConfiguration clonedConfig = new DatadogLogCVConfiguration();
    super.copy(clonedConfig);
    clonedConfig.setHostnameField(this.getHostnameField());
    return clonedConfig;
  }
}
