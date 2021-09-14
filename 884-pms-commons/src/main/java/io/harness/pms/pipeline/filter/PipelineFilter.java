/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
