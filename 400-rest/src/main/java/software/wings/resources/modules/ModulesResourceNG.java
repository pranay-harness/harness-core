/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.resources.modules;

import io.harness.ModuleType;
import io.harness.beans.FeatureName;
import io.harness.ff.FeatureFlagService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;

@Api(value = "/ng/modules", hidden = true)
@Path("/ng/modules")
@Produces("application/json")
@Consumes("application/json")
@NextGenManagerAuth
@Slf4j
public class ModulesResourceNG {
  private final FeatureFlagService featureFlagService;
  private final Map<ModuleType, FeatureName> moduleTypeFeatureNameMap;

  @Inject
  public ModulesResourceNG(FeatureFlagService featureFlagService) {
    this.featureFlagService = featureFlagService;
    this.moduleTypeFeatureNameMap = getModuleTypeFeatureNameMap();
  }

  private Map<ModuleType, FeatureName> getModuleTypeFeatureNameMap() {
    Map<ModuleType, FeatureName> modules = new HashMap<>();
    modules.put(ModuleType.CD, FeatureName.CDNG_ENABLED);
    modules.put(ModuleType.CE, FeatureName.CENG_ENABLED);
    modules.put(ModuleType.CF, FeatureName.CFNG_ENABLED);
    modules.put(ModuleType.CI, FeatureName.CING_ENABLED);
    modules.put(ModuleType.CV, FeatureName.CVNG_ENABLED);
    return modules;
  }

  @GET
  public RestResponse<List<ModuleType>> getEnabledModules(@NotNull @QueryParam("accountId") String accountId) {
    List<ModuleType> enabledModuleTypes = new ArrayList<>();
    moduleTypeFeatureNameMap.forEach((moduleType, featureName) -> {
      if (featureFlagService.isEnabled(featureName, accountId)) {
        enabledModuleTypes.add(moduleType);
      }
    });
    return new RestResponse<>(enabledModuleTypes);
  }
}
