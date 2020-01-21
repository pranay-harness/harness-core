package software.wings.service.impl.instana;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InstanaMetricValues {
  List<List<Number>> values;
}