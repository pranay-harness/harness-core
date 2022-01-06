/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.verification.dynatrace;

import software.wings.verification.CVConfiguration;
import software.wings.verification.CVConfigurationYaml;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Created by Pranjal on 10/16/2018
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class DynaTraceCVServiceConfiguration extends CVConfiguration {
  private String serviceEntityId;

  @Override
  public CVConfiguration deepCopy() {
    DynaTraceCVServiceConfiguration clonedConfig = new DynaTraceCVServiceConfiguration();
    super.copy(clonedConfig);
    clonedConfig.setServiceEntityId(this.serviceEntityId);
    return clonedConfig;
  }

  /**
   * The type Yaml.
   */
  @Data
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  @Builder
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class DynaTraceCVConfigurationYaml extends CVConfigurationYaml {
    private String dynatraceServiceName;
    private String dynatraceServiceEntityId;
  }
}
