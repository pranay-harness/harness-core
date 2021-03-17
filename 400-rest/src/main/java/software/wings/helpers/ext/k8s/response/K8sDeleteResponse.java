package software.wings.helpers.ext.k8s.response;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
public class K8sDeleteResponse implements K8sTaskResponse {}
