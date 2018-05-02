package software.wings.beans;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class APMValidateCollectorConfig {
  private String baseUrl;
  private String url;
  private Map<String, String> headers;
  private Map<String, String> options;
}
