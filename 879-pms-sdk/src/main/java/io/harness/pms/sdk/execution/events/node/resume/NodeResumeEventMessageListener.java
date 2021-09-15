/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.execution.events.node.resume;

import static io.harness.pms.sdk.PmsSdkModuleUtils.CORE_EXECUTOR_NAME;
import static io.harness.pms.sdk.PmsSdkModuleUtils.SDK_SERVICE_NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.resume.NodeResumeEvent;
import io.harness.pms.events.base.PmsAbstractMessageListener;
import io.harness.pms.sdk.core.execution.events.node.resume.NodeResumeEventHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class NodeResumeEventMessageListener
    extends PmsAbstractMessageListener<NodeResumeEvent, NodeResumeEventHandler> {
  @Inject
  public NodeResumeEventMessageListener(@Named(SDK_SERVICE_NAME) String serviceName,
      NodeResumeEventHandler nodeResumeEventHandler, @Named(CORE_EXECUTOR_NAME) ExecutorService executorService) {
    super(serviceName, NodeResumeEvent.class, nodeResumeEventHandler, executorService);
  }
}
