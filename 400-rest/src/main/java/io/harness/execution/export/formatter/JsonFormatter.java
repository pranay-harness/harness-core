/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.formatter;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExportExecutionsException;
import io.harness.execution.export.metadata.ExecutionMetadata;
import io.harness.serializer.JsonSubtypeResolver;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@OwnedBy(CDC)
public class JsonFormatter implements OutputFormatter {
  private static final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.setSubtypeResolver(new JsonSubtypeResolver(mapper.getSubtypeResolver()));
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new GuavaModule());
    mapper.registerModule(new JavaTimeModule());
  }

  public String getOutputString(Object obj) {
    try {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    } catch (Exception ex) {
      throw new ExportExecutionsException("Unknown error while generating JSON", ex);
    }
  }

  public byte[] getOutputBytes(Object obj) {
    try {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(obj);
    } catch (Exception ex) {
      throw new ExportExecutionsException("Unknown error while generating JSON", ex);
    }
  }

  @Override
  public byte[] getExecutionMetadataOutputBytes(ExecutionMetadata executionMetadata) {
    return getOutputBytes(executionMetadata);
  }

  @Override
  public String getExtension() {
    return "json";
  }
}
