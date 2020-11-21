package io.harness.ngtriggers.beans.config;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HeaderConfig {
  private String key;
  private List<String> values;
}
