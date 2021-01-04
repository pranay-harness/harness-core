package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * The type Web hook request.
 */
@OwnedBy(CDC)
@Data
@Builder
public class WebHookRequest {
  @NotEmpty private String application;
  private List<Map<String, Object>> artifacts;
  private List<Map<String, Object>> manifests;
  private Map<String, String> parameters;
}
