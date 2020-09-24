package io.harness.walktree.visitor.response;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class VisitorErrorResponseWrapper implements VisitorResponse {
  @Builder.Default List<VisitorErrorResponse> errors = new ArrayList<>();

  public void add(VisitorResponse visitorResponse) {
    VisitorErrorResponseWrapper visitorErrorResponseWrapper = (VisitorErrorResponseWrapper) visitorResponse;
    this.errors.addAll(visitorErrorResponseWrapper.getErrors());
  }
}
