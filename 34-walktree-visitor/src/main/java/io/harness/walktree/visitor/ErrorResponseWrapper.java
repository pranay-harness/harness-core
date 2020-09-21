package io.harness.walktree.visitor;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ErrorResponseWrapper {
  @Builder.Default List<ErrorResponse> errors = new ArrayList<>();

  public void add(ErrorResponseWrapper errorResponseWrapper) {
    this.errors.addAll(errorResponseWrapper.getErrors());
  }
}
