/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.verification.log;

import software.wings.stencils.DefaultValue;
import software.wings.verification.CVConfiguration;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.reinert.jjschema.Attributes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

/**
 * Created by Pranjal on 06/04/2019
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "StackdriverCVConfigurationKeys")
public class StackdriverCVConfiguration extends LogsCVConfiguration {
  @Attributes(title = "Is Log Configuration", required = true)
  @DefaultValue("true")
  private boolean isLogsConfiguration;

  @Attributes(required = true, title = "Host Name Field") @DefaultValue("pod_id") protected String hostnameField;

  @Attributes(required = true, title = "Log Message Field") @DefaultValue("textPayload") protected String messageField;

  @Override
  public CVConfiguration deepCopy() {
    StackdriverCVConfiguration clonedConfig = new StackdriverCVConfiguration();
    super.copy(clonedConfig);
    clonedConfig.setHostnameField(this.getHostnameField());
    clonedConfig.setLogsConfiguration(this.isLogsConfiguration);
    clonedConfig.setMessageField(this.getMessageField());
    clonedConfig.setQuery(this.getQuery());
    return clonedConfig;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  public static class StackdriverCVConfigurationYaml extends LogsCVConfigurationYaml {
    private String hostnameField;
    private String messageField;
    private boolean isLogsConfiguration;
  }
}
