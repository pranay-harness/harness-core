package io.harness.ngtriggers.beans.config;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("headerConfig")
public class HeaderConfig {
  private String key;
  private List<String> values;
}
