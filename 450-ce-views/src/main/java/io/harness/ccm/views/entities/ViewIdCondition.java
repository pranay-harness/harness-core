package io.harness.ccm.views.entities;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

import java.util.List;

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
