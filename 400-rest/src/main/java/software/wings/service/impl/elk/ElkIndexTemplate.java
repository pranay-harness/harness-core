package software.wings.service.impl.elk;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.Map;
import lombok.Data;

/**
 * Created by rsingh on 8/23/17.
 */
@Data
@OwnedBy(HarnessTeam.CV)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class ElkIndexTemplate {
  private String name;
  private Map<String, Object> properties;
}
