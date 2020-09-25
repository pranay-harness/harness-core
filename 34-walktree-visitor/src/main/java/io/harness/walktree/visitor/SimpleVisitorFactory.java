package io.harness.walktree.visitor;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.walktree.visitor.inputset.InputSetTemplateVisitor;
import io.harness.walktree.visitor.mergeinputset.MergeInputSetVisitor;
import io.harness.walktree.visitor.mergeinputset.beans.MergeVisitorInputSetElement;
import io.harness.walktree.visitor.validation.ValidationVisitor;

import java.util.List;

@Singleton
public class SimpleVisitorFactory {
  @Inject Injector injector;

  public ValidationVisitor obtainValidationVisitor(Class<?> modeType, boolean useFQN) {
    ValidationVisitor validationVisitor = new ValidationVisitor(injector, modeType, useFQN);
    injector.injectMembers(validationVisitor);
    return validationVisitor;
  }

  public MergeInputSetVisitor obtainMergeInputSetVisitor(
      boolean useFQN, List<MergeVisitorInputSetElement> inputSetPipelineList) {
    MergeInputSetVisitor mergeInputSetVisitor = new MergeInputSetVisitor(injector, useFQN, inputSetPipelineList);
    injector.injectMembers(mergeInputSetVisitor);
    return mergeInputSetVisitor;
  }

  public InputSetTemplateVisitor obtainInputSetTemplateVisitor() {
    InputSetTemplateVisitor inputSetTemplateVisitor = new InputSetTemplateVisitor(injector);
    injector.injectMembers(inputSetTemplateVisitor);
    return inputSetTemplateVisitor;
  }
}
