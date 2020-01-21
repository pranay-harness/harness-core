package software.wings.service.impl.instana;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class InstanaMetricItem {
  private long from, to;
  private String label;
  private Map<String, List<List<Number>>> metrics;
}