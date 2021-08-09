package io.harness.pms.pipeline.filter;

import io.harness.exception.GeneralException;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface PipelineFilter {
  default String toJson() {
    try {
      return new ObjectMapper().writer().writeValueAsString(this);
    } catch (Exception ex) {
      throw new GeneralException("Unknown error while generating JSON", ex);
    }
  }
}
