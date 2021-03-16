package software.wings.core.managerConfiguration;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.util.List;

@TargetModule(Module._960_PERSISTENCE)
public interface ConfigChangeListener {
  void onConfigChange(List<ConfigChangeEvent> events);
}
