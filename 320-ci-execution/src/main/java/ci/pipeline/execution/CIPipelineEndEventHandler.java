/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package ci.pipeline.execution;

import io.harness.ci.plan.creator.execution.CIPipelineModuleInfo;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.pms.sdk.execution.beans.PipelineModuleInfo;
import io.harness.repositories.CIAccountExecutionMetadataRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CIPipelineEndEventHandler implements OrchestrationEventHandler {
  @Inject CIAccountExecutionMetadataRepository ciAccountExecutionMetadataRepository;
  @Override
  public void handleEvent(OrchestrationEvent event) {
    PipelineModuleInfo moduleInfo = event.getModuleInfo();
    if (moduleInfo instanceof CIPipelineModuleInfo) {
      if (((CIPipelineModuleInfo) moduleInfo).getIsPrivateRepo()) {
        ciAccountExecutionMetadataRepository.updateAccountExecutionMetadata(
            AmbianceUtils.getAccountId(event.getAmbiance()), event.getEndTs());
      }
    }
  }
}
