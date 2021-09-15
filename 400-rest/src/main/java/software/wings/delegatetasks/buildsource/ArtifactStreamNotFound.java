/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.delegatetasks.buildsource;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(CDC)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class ArtifactStreamNotFound extends RuntimeException {
  public ArtifactStreamNotFound(String artifactStreamId) {
    super(String.format("ArtifactServer %s could not be found", artifactStreamId));
  }
}
