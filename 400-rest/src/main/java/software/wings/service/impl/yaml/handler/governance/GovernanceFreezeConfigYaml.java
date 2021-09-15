/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.handler.governance;

import io.harness.governance.TimeRangeBasedFreezeConfig;

import software.wings.yaml.BaseYamlWithType;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes(
    { @JsonSubTypes.Type(value = TimeRangeBasedFreezeConfig.Yaml.class, name = "TIME_RANGE_BASED_FREEZE_CONFIG") })
public abstract class GovernanceFreezeConfigYaml extends BaseYamlWithType {
  public GovernanceFreezeConfigYaml(String type) {
    super(type);
  }
}
