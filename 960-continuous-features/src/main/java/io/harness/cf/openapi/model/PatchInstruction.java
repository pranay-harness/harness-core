package io.harness.cf.openapi.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PatchInstruction {
  private String kind;
  private Object parameters;
}
