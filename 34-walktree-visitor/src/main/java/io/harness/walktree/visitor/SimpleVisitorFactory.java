package io.harness.walktree.visitor;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.walktree.visitor.mergeinputset.MergeInputSetVisitor;
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

  public MergeInputSetVisitor obtainMergeInputSetVisitor(boolean useFQN, List<Object> inputSetsPipeline) {
    MergeInputSetVisitor mergeInputSetVisitor = new MergeInputSetVisitor(injector, useFQN, inputSetsPipeline);
    injector.injectMembers(mergeInputSetVisitor);
    return mergeInputSetVisitor;
  }
}
