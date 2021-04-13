package io.harness.cf.openapi.api;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;

@Builder
public class PatchOperation {
  List<PatchInstruction> instructions = new ArrayList<>();
}
