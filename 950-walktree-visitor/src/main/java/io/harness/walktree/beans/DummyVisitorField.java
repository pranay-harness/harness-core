/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.walktree.beans;

import io.harness.walktree.registries.visitorfield.VisitorFieldType;
import io.harness.walktree.registries.visitorfield.VisitorFieldWrapper;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DummyVisitorField implements VisitorFieldWrapper {
  public static final VisitorFieldType VISITOR_FIELD_TYPE = VisitorFieldType.builder().type("DUMMY_FIELD").build();

  String value;
  @Override
  public VisitorFieldType getVisitorFieldType() {
    return VisitorFieldType.builder().type("DUMMY").build();
  }
}
