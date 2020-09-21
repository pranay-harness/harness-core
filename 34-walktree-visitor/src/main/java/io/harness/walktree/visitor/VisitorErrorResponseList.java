package io.harness.walktree.visitor;

import io.harness.walktree.visitor.response.VisitorResponse;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class VisitorErrorResponseList implements VisitorResponse {
  @Builder.Default List<VisitorErrorResponse> errors = new ArrayList<>();

  public void add(VisitorResponse visitorResponse) {
    VisitorErrorResponseList visitorErrorResponseList = (VisitorErrorResponseList) visitorResponse;
    this.errors.addAll(visitorErrorResponseList.getErrors());
  }
}
