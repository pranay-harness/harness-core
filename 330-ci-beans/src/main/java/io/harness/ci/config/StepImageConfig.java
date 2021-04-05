package io.harness.ci.config;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StepImageConfig {
  String image;
  List<String> entrypoint;
}
