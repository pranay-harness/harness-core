/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.yaml.trigger;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.CDC)
@Data
@JsonTypeName("NEW_MANIFEST")
@Builder
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public class ManifestTriggerConditionYaml extends TriggerConditionYaml {
  private String serviceName;
  private String versionRegex;

  public ManifestTriggerConditionYaml() {
    super.setType("NEW_MANIFEST");
  }

  public ManifestTriggerConditionYaml(String serviceName, String versionRegex) {
    this();
    this.serviceName = serviceName;
    this.versionRegex = versionRegex;
  }
}
