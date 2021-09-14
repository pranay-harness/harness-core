/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.walktree.visitor;

import io.harness.walktree.visitor.entityreference.EntityReferenceExtractorVisitor;
import io.harness.walktree.visitor.inputset.InputSetTemplateVisitor;
import io.harness.walktree.visitor.mergeinputset.MergeInputSetVisitor;
import io.harness.walktree.visitor.mergeinputset.beans.MergeVisitorInputSetElement;
import io.harness.walktree.visitor.validation.ValidationVisitor;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
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

  public InputSetTemplateVisitor obtainInputSetTemplateVisitor(boolean keepRuntimeInput) {
    InputSetTemplateVisitor inputSetTemplateVisitor = new InputSetTemplateVisitor(injector, keepRuntimeInput);
    injector.injectMembers(inputSetTemplateVisitor);
    return inputSetTemplateVisitor;
  }

  public EntityReferenceExtractorVisitor obtainEntityReferenceExtractorVisitor(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, List<String> qualifiedNameLists) {
    EntityReferenceExtractorVisitor entityReferenceExtractorVisitor = new EntityReferenceExtractorVisitor(
        injector, accountIdentifier, orgIdentifier, projectIdentifier, qualifiedNameLists);
    injector.injectMembers(entityReferenceExtractorVisitor);
    return entityReferenceExtractorVisitor;
  }
}
