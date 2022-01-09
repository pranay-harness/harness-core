/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.yaml;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.yaml.BaseYaml;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(DX)
public abstract class BaseYamlWithType extends BaseYaml {
  /**
   * There are several types at different levels.
   * For example, at the root level, we have APP, SERVICE, WORKFLOW etc.
   * Each root type can have sub-types, for example WORKFLOW can have Canary, Multi-Service,
   * etc which are all modeled as different yamls.
   * The type reflects the sub type.
   */
  private String type;

  public BaseYamlWithType(String type) {
    this.type = type;
  }
}
