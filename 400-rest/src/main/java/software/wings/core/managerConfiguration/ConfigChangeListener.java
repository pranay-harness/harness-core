package software.wings.core.managerConfiguration;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.List;

@TargetModule(HarnessModule._960_PERSISTENCE)
public interface ConfigChangeListener {
  void onConfigChange(List<ConfigChangeEvent> events);
}
