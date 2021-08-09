package io.harness.ccm.views.entities;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({ @JsonSubTypes.Type(value = ViewIdCondition.class, name = "VIEW_ID_CONDITION") })
public abstract class ViewCondition {
  String type;

  public ViewCondition(String type) {
    this.type = type;
  }
}
