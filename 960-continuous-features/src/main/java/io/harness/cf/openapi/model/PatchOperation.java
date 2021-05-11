package io.harness.cf.openapi.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

@Data
@Builder
public class PatchOperation {
  @Default private List<PatchInstruction> instructions = new ArrayList<>();
}
