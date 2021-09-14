/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.walktree.visitor.validation;

import io.harness.walktree.visitor.DummyVisitableElement;

public interface ConfigValidator extends DummyVisitableElement {
  /**
   * used to do object specific validation.
   *
   * The error is stored in visitor.errorMap as uuid -> ValidationErrors
   * The uuid should be present in one of the field.
   *
   * @param object
   * @param visitor
   */
  void validate(Object object, ValidationVisitor visitor);
}
