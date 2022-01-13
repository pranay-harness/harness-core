/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.workflow.creation.abstractfactories;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.intfc.workflow.creation.WorkflowCreatorFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class AbstractWorkflowFactory {
  @Inject private K8sV2WorkflowFactory k8sV2WorkflowFactory;
  @Inject private GeneralWorkflowFactory generalWorkflowFactory;

  public enum Category {
    K8S_V2,
    GENERAL;
  }

  public WorkflowCreatorFactory getWorkflowCreatorFactory(Category category) {
    switch (category) {
      case K8S_V2:
        return k8sV2WorkflowFactory;
      case GENERAL:
      default:
        return generalWorkflowFactory;
    }
  }
}
