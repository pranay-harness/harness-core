/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.helpers.ext.jenkins.model;

import static io.harness.annotations.dev.HarnessModule._960_API_SERVICES;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.offbytwo.jenkins.model.BaseModel;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(HarnessTeam.CDC)
@TargetModule(_960_API_SERVICES)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class ParametersDefinitionProperty extends BaseModel {
  private String name;
  private String description;
  private DefaultParameterValue defaultParameterValue;
  private String type;
  private List<String> choices;

  @Data
  @Builder
  public static class DefaultParameterValue {
    private String name;
    private String value;
  }
}
