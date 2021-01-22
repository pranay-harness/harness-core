package io.harness.pms.sdk;

import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.plan.PlanCreationServiceGrpc;
import io.harness.pms.contracts.plan.YamlFieldBlob;
import io.harness.pms.plan.creation.PlanCreatorServiceInfo;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.yaml.YamlField;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class PmsSdkHelper {
  @Inject private Map<String, PlanCreationServiceGrpc.PlanCreationServiceBlockingStub> planCreatorServices;
  @Inject private PmsSdkInstanceService pmsSdkInstanceService;

  /**
   * Gets the list of registered services with their PlanCreatorServiceInfo object
   * @return
   */
  public Map<String, PlanCreatorServiceInfo> getServices() {
    Map<String, Map<String, Set<String>>> sdkInstances = pmsSdkInstanceService.getInstanceNameToSupportedTypes();
    Map<String, PlanCreatorServiceInfo> services = new HashMap<>();
    if (EmptyPredicate.isNotEmpty(planCreatorServices) && EmptyPredicate.isNotEmpty(sdkInstances)) {
      sdkInstances.forEach((k, v) -> {
        if (planCreatorServices.containsKey(k)) {
          services.put(k, new PlanCreatorServiceInfo(v, planCreatorServices.get(k)));
        }
      });
    }
    return services;
  }

  /**
   * Checks if the service supports any of the dependency mentioned.
   * @param serviceInfo
   * @param dependencies
   * @return
   */
  public boolean containsSupportedDependency(
      PlanCreatorServiceInfo serviceInfo, Map<String, YamlFieldBlob> dependencies) {
    Map<String, Set<String>> supportedTypes = serviceInfo.getSupportedTypes();
    Map<String, YamlFieldBlob> filteredDependencies =
        dependencies.entrySet()
            .stream()
            .filter(entry -> {
              try {
                YamlField field = YamlField.fromFieldBlob(entry.getValue());
                return PlanCreatorUtils.supportsField(supportedTypes, field);
              } catch (Exception e) {
                log.error("Invalid yaml field", e);
                return false;
              }
            })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return !EmptyPredicate.isEmpty(filteredDependencies);
  }
}
