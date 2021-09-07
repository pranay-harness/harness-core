package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * The type Web hook request.
 */
@TargetModule(HarnessModule._957_CG_BEANS)
@OwnedBy(CDC)
@Data
@Builder
public class WebHookRequest {
  @NotEmpty private String application;
  private List<Map<String, Object>> artifacts;
  private List<Map<String, Object>> manifests;
  private Map<String, String> parameters;
}
