package software.wings.beans;

import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * The type Web hook request.
 */
@Builder
@Data
public class WebHookRequest {
  @NotEmpty private String application;
  @NotEmpty private String artifactSource;
  private String buildNumber;
  private String imageTag;
}
