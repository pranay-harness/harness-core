package io.harness.walktree.visitor.validation;

import io.harness.walktree.visitor.validation.annotations.Required;
import io.harness.walktree.visitor.validation.modes.PreInputSet;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VisitorTestChild {
  @Required(groups = PreInputSet.class) private String name;
}
