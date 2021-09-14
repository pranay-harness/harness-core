/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
