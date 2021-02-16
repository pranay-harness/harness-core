package io.harness.cvng.metrics.beans;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CVNGMetricGroup {
  String name;
  String identifier;
  List<String> labels;
}
