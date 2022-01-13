/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.yaml.BaseYamlWithType;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._957_CG_BEANS)
@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class VariableYaml extends BaseYamlWithType {
  private String name;
  private String description;
  private boolean mandatory;
  private String value;
  private boolean fixed;
  private String allowedValues; // todo: toYaml() don't convert to allowedValues
  private List<AllowedValueYaml> allowedList;

  @Builder
  public VariableYaml(String type, String name, String description, boolean mandatory, String value, boolean fixed,
      String allowedValues, List<AllowedValueYaml> allowedList) {
    super(type);
    this.name = name;
    this.description = description;
    this.mandatory = mandatory;
    this.value = value;
    this.fixed = fixed;
    this.allowedValues = allowedValues;
    this.allowedList = allowedList;
  }
}
