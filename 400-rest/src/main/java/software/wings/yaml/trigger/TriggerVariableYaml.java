/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.AllowedValueYaml;
import software.wings.beans.NameValuePair;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public class TriggerVariableYaml extends NameValuePair.AbstractYaml {
  String entityType;

  @Builder
  public TriggerVariableYaml(
      String entityType, String name, String value, String valueType, List<AllowedValueYaml> allowedValueYamls) {
    super(name, value, valueType, allowedValueYamls);
    this.entityType = entityType;
  }
}
