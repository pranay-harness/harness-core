package io.harness.ccm.views.entities;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({ @JsonSubTypes.Type(value = ViewIdCondition.class, name = "VIEW_ID_CONDITION") })
@Schema(description =
            "This object defines a filter Condition, an array of filter Conditions are combined using AND operator")
public abstract class ViewCondition {
  String type;

  public ViewCondition(String type) {
    this.type = type;
  }
}
