package software.wings.beans;

import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.Map;

/**
 * The type Web hook request.
 */
@Builder
@Data
public class WebHookRequest {
  @NotEmpty private String application;
  @NotEmpty private String artifactSource;
  private String buildNumber;
  private String dockerImageTag;
  private Map<String, String> parameters;
}
