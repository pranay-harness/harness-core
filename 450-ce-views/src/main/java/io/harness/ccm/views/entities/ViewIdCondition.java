/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.views.entities;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonTypeName("VIEW_ID_CONDITION")
@EqualsAndHashCode(callSuper = false)
public class ViewIdCondition extends ViewCondition {
  ViewField viewField;
  ViewIdOperator viewOperator;
  List<String> values;

  public ViewIdCondition() {
    super("VIEW_ID_CONDITION");
  }

  public ViewIdCondition(ViewField viewField, ViewIdOperator viewOperator, List<String> values) {
    this();
    this.viewField = viewField;
    this.viewOperator = viewOperator;
    this.values = values;
  }
}
