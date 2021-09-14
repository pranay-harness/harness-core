/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.perpetualtask.k8s.informer.handlers;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.event.client.EventPublisher;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;

import io.kubernetes.client.openapi.models.V1Node;

@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public class V1NodeHandler extends BaseHandler<V1Node> {
  public V1NodeHandler(EventPublisher eventPublisher, ClusterDetails clusterDetails) {
    super(eventPublisher, clusterDetails);
  }

  @Override
  String getKind() {
    return "Node";
  }

  @Override
  String getApiVersion() {
    return "v1";
  }
}
