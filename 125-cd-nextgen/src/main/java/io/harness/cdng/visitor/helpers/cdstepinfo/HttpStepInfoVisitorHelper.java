package io.harness.cdng.visitor.helpers.cdstepinfo;

import io.harness.cdng.pipeline.stepinfo.HttpStepInfo;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class HttpStepInfoVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    HttpStepInfo httpStepInfo = (HttpStepInfo) originalElement;
    return HttpStepInfo.infoBuilder().identifier(httpStepInfo.getIdentifier()).build();
  }
}
