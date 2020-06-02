package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

/**
 * The type Web hook request.
 */
@OwnedBy(CDC)
@Data
@Builder
public class WebHookRequest {
  @NotEmpty private String application;
  private List<Map<String, Object>> artifacts;
  private Map<String, String> parameters;
}
