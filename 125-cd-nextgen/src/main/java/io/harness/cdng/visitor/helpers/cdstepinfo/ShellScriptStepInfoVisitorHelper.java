package io.harness.cdng.visitor.helpers.cdstepinfo;

import io.harness.cdng.pipeline.stepinfo.ShellScriptStepInfo;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class ShellScriptStepInfoVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement() {
    return ShellScriptStepInfo.infoBuilder().build();
  }
}
