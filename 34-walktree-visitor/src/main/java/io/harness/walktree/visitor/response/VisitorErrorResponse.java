package io.harness.walktree.visitor.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VisitorErrorResponse {
  String fieldName;
  String message;
}
