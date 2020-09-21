package io.harness.walktree.visitor;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VisitorErrorResponse {
  String fieldName;
  String message;
}
