package io.harness.ccm.views.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false)
@Schema(description =
            "This object will contain a Perspective Rule, an array of Perspective Rules are combined using OR operator")
public class ViewRule {
  List<ViewCondition> viewConditions;
}
