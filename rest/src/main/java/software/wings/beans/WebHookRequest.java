package software.wings.beans;

import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

/**
 * The type Web hook request.
 */
@Builder
@Data
public class WebHookRequest {
  @NotEmpty private String application;
  private List<Map<String, String>> artifacts;
  private Map<String, String> parameters;
}
