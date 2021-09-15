/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

/**
 * Created by sgurubelli on 11/20/17.
 */
@Data
@Builder
@OwnedBy(HarnessTeam.CDC)
public class BuildExecutionSummary {
  String artifactStreamId;
  String artifactSource;
  String revision;
  String buildUrl;
  String buildName;
  String metadata;
}
