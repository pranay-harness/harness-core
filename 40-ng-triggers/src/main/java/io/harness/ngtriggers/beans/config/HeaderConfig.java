package io.harness.ngtriggers.beans.config;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HeaderConfig {
  private String key;
  private List<String> values;
}
