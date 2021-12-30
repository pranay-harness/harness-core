package software.wings.sm.states.rancher;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.annotations.dev.TargetModule;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.RancherKubernetesInfrastructureMapping;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.sm.ExecutionContext;

import static io.harness.annotations.dev.HarnessModule._870_CG_ORCHESTRATION;

@Singleton
@Slf4j
@TargetModule(_870_CG_ORCHESTRATION)
public class RancherStateHelper {

  @Inject
  private transient InfrastructureMappingService infrastructureMappingService;

  public RancherKubernetesInfrastructureMapping fetchRancherKubernetesInfrastructureMapping(ExecutionContext context) {
    return (RancherKubernetesInfrastructureMapping) infrastructureMappingService.get(
            context.getAppId(), context.fetchInfraMappingId());
  }
}
