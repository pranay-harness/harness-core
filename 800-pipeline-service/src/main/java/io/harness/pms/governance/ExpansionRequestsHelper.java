package io.harness.pms.governance;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.PmsSdkInstance;
import io.harness.pms.sdk.PmsSdkInstanceService;
import io.harness.pms.yaml.YAMLFieldNameConstants;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(PIPELINE)
@Singleton
public class ExpansionRequestsHelper {
  @Inject PmsSdkInstanceService pmsSdkInstanceService;

  public Map<ModuleType, Set<String>> getExpandableFieldsPerService() {
    List<PmsSdkInstance> activeInstances = pmsSdkInstanceService.getActiveInstances();
    Map<ModuleType, Set<String>> expandableFieldsPerService = new HashMap<>();
    activeInstances.forEach(sdkInstance -> {
      String sdkInstanceName = sdkInstance.getName();
      ModuleType module = ModuleType.fromString(sdkInstanceName);
      Set<String> expandableFields = new HashSet<>(sdkInstance.getExpandableFields());
      expandableFieldsPerService.put(module, expandableFields);
    });
    return expandableFieldsPerService;
  }

  public Map<String, ModuleType> getTypeToService() {
    List<PmsSdkInstance> activeInstances = pmsSdkInstanceService.getActiveInstances();
    Map<String, ModuleType> typeToModule = new HashMap<>();
    activeInstances.forEach(sdkInstance -> {
      String sdkInstanceName = sdkInstance.getName();
      ModuleType module = ModuleType.fromString(sdkInstanceName);
      Map<String, Set<String>> supportedTypes = sdkInstance.getSupportedTypes();
      Set<String> supportedStageTypes = supportedTypes.getOrDefault(YAMLFieldNameConstants.STAGE, new HashSet<>());
      Set<String> supportedStepTypes = supportedTypes.getOrDefault(YAMLFieldNameConstants.STEP, new HashSet<>());
      supportedStageTypes.forEach(type -> typeToModule.put(type, module));
      supportedStepTypes.forEach(type -> typeToModule.put(type, module));
    });
    return typeToModule;
  }
}
