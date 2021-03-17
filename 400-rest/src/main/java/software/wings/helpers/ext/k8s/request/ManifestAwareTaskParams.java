package software.wings.helpers.ext.k8s.request;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
public interface ManifestAwareTaskParams {
  K8sDelegateManifestConfig getK8sDelegateManifestConfig();
}
