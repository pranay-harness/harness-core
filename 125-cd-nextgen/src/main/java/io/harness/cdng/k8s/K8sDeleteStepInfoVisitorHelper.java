package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

@OwnedBy(CDP)
public class K8sDeleteStepInfoVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    K8sDeleteStepInfo k8sScaleStepInfo = (K8sDeleteStepInfo) originalElement;
    return K8sDeleteStepInfo.infoBuilder().identifier(k8sScaleStepInfo.getIdentifier()).build();
  }
}
