/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.governance;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
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
      List<String> expandableFieldsList = sdkInstance.getExpandableFields();
      Set<String> expandableFields =
          EmptyPredicate.isEmpty(expandableFieldsList) ? new HashSet<>() : new HashSet<>(expandableFieldsList);
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
