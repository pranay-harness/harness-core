package io.harness.walktree.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VisitableChild {
  String fieldName;
  Object value;
}
