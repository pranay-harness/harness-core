/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.yaml.setting;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.security.UsageRestrictions;
import software.wings.settings.SettingValue;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author rktummala on 11/18/17
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.CDP)
public abstract class CloudProviderYaml extends SettingValue.Yaml {
  public CloudProviderYaml(String type, String harnessApiVersion, UsageRestrictions.Yaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
  }
}
