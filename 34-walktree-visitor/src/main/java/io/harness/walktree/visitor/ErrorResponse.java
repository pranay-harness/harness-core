package io.harness.walktree.visitor;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorResponse {
  String fieldName;
  String message;
}
