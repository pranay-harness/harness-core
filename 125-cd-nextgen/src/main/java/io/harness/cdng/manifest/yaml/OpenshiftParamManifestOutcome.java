/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.manifest.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("openshiftParamManifestOutcome")
@JsonTypeName(ManifestType.OpenshiftParam)
@OwnedBy(CDP)
@RecasterAlias("io.harness.cdng.manifest.yaml.OpenshiftParamManifestOutcome")
public class OpenshiftParamManifestOutcome implements ManifestOutcome {
  String identifier;
  String type = ManifestType.OpenshiftParam;
  StoreConfig store;
  int order;
}
