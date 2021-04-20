package io.harness.dto;

import io.harness.delegate.beans.DelegateSelectionLogParams;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GraphDelegateSelectionLogParams {
  String taskId;
  String taskName;
  DelegateSelectionLogParams selectionLogParams;
}
