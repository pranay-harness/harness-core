package io.harness.ngpipeline.visitor.helpers.ngpipeline;

import io.harness.annotations.dev.ToBeDeleted;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

@ToBeDeleted
@Deprecated
public class NgPipelineVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Will be taken by Validation framework impl.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    NgPipeline ngPipeline = (NgPipeline) originalElement;
    return NgPipeline.builder().identifier(ngPipeline.getIdentifier()).build();
  }
}
