/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask.k8s.watch;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import io.kubernetes.client.openapi.models.V1ObjectMeta;
import javax.annotation.Nullable;
import lombok.Value;

@OwnedBy(HarnessTeam.CE)
@Value(staticConstructor = "of")
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public class Workload {
  String kind;
  V1ObjectMeta objectMeta;
  @Nullable Integer replicas;
}
