package io.harness.skip.skipper.impl;

import io.harness.beans.GraphVertex;
import io.harness.dto.OrchestrationGraph;
import io.harness.skip.skipper.VertexSkipper;

public class NoOpSkipper extends VertexSkipper {
  @Override
  public void skip(OrchestrationGraph orchestrationGraph, GraphVertex skippedVertex) {
    // no op
  }
}
