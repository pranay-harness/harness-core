/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
