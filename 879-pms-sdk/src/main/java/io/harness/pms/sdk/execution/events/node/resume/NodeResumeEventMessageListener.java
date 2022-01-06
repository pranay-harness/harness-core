/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
