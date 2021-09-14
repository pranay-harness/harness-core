/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@OwnedBy(CDC)
@Data
@Builder
@TargetModule(HarnessModule._957_CG_BEANS)
public class WorkflowNotificationDetails {
  @Builder.Default private String url = "";
  @Builder.Default private String name = "";
  @Builder.Default private String message = "";
}
