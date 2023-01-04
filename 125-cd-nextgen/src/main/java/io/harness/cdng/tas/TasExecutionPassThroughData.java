/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.tas;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.task.pcf.PcfManifestsPackage;
import io.harness.pcf.model.CfCliVersionNG;
import io.harness.pms.sdk.core.steps.io.PassThroughData;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder(toBuilder = true)
@OwnedBy(CDP)
@TypeAlias("tasExecutionPassThroughData")
@RecasterAlias("io.harness.cdng.tas.TasExecutionPassThroughData")
public class TasExecutionPassThroughData implements PassThroughData {
  String applicationName;
  InfrastructureOutcome infrastructure;
  UnitProgressData lastActiveUnitProgressData;
  String zippedManifestId;
  PcfManifestsPackage pcfManifestsPackage;
  Map<String, String> allFilesFetched;
  String repoRoot;
  CfCliVersionNG cfCliVersion;
  String rawScript;
  List<String> commandUnits;
  List<String> pathsFromScript;
}
