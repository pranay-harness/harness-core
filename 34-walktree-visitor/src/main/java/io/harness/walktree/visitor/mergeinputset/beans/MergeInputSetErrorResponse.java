package io.harness.walktree.visitor.mergeinputset.beans;

import io.harness.walktree.visitor.response.VisitorErrorResponse;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class MergeInputSetErrorResponse extends VisitorErrorResponse {
  String causedByInputSetIdentifier;

  @Builder(builderMethodName = "mergeErrorBuilder")
  public MergeInputSetErrorResponse(String fieldName, String message, String causedByInputSetIdentifier) {
    super(fieldName, message);
    this.causedByInputSetIdentifier = causedByInputSetIdentifier;
  }
}
