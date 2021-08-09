package io.harness.walktree.visitor.response;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VisitorErrorResponse {
  String fieldName;
  String message;

  @Builder(builderMethodName = "errorBuilder")
  public VisitorErrorResponse(String fieldName, String message) {
    this.fieldName = fieldName;
    this.message = message;
  }
}
