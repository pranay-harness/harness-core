/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.infrastructure.instance.key.deployment;

import static java.lang.String.format;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AzureWebAppDeploymentKey extends DeploymentKey {
  private String appName;
  private String slotName;

  @Getter(AccessLevel.NONE) private String key;

  public String getKey() {
    return format("%s_%s", appName, slotName);
  }
}
