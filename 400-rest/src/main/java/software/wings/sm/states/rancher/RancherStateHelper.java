package software.wings.sm.states.rancher;

import static io.harness.annotations.dev.HarnessModule._870_CG_ORCHESTRATION;

import io.harness.annotations.dev.TargetModule;

import software.wings.beans.RancherKubernetesInfrastructureMapping;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.sm.ExecutionContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@TargetModule(_870_CG_ORCHESTRATION)
public class RancherStateHelper {
  @Inject private transient InfrastructureMappingService infrastructureMappingService;

  public RancherKubernetesInfrastructureMapping fetchRancherKubernetesInfrastructureMapping(ExecutionContext context) {
    return (RancherKubernetesInfrastructureMapping) infrastructureMappingService.get(
        context.getAppId(), context.fetchInfraMappingId());
  }
}
