package software.wings.service.impl.workflow.creation.abstractfactories;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.service.intfc.workflow.creation.WorkflowCreatorFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.CDC)
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
