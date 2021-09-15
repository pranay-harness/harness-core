/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.handler.governance;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.governance.CustomAppFilter;
import io.harness.governance.CustomEnvFilter;
import io.harness.governance.EnvironmentFilter;
import io.harness.governance.EnvironmentFilterYaml;

import software.wings.beans.Application;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.intfc.AppService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class CustomAppFilterYamlHandler extends ApplicationFilterYamlHandler<CustomAppFilter.Yaml, CustomAppFilter> {
  @Inject private AppService appService;

  @Inject YamlHandlerFactory yamlHandlerFactory;

  @Override
  public CustomAppFilter.Yaml toYaml(CustomAppFilter bean, String accountId) {
    List<String> appNames = appService.getAppsByIds(new HashSet<String>(bean.getApps()))
                                .stream()
                                .map(Application::getName)
                                .collect(Collectors.toList());

    EnvironmentFilterYamlHandler environmentFilterYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.ENV_FILTER, bean.getEnvSelection().getFilterType().name());

    // envSelection is made a List to make Yaml cleanup work YamlUtils.cleanUpDoubleExclamationLines
    return CustomAppFilter.Yaml.builder()
        .apps(appNames)
        .envSelection(Arrays.asList(environmentFilterYamlHandler.toYaml(bean.getEnvSelection(), accountId)))
        .filterType(bean.getFilterType())
        .build();
  }

  @Override
  public CustomAppFilter upsertFromYaml(
      ChangeContext<CustomAppFilter.Yaml> changeContext, List<ChangeContext> changeSetContext) {
    CustomAppFilter customAppFilter = CustomAppFilter.builder().build();
    toBean(customAppFilter, changeContext, changeSetContext);
    return customAppFilter;
  }

  @Override
  public Class getYamlClass() {
    return CustomAppFilter.Yaml.class;
  }

  private void toBean(
      CustomAppFilter bean, ChangeContext<CustomAppFilter.Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String accountId = changeContext.getChange().getAccountId();

    CustomAppFilter.Yaml yaml = changeContext.getYaml();

    EnvironmentFilterYamlHandler environmentFilterYamlHandler;
    List<String> appIds = getAppIds(accountId, yaml.getApps());

    // // envSelection is made a List to make Yaml cleanup work YamlUtils.cleanUpDoubleExclamationLines
    List<EnvironmentFilter> environmentFilters = new ArrayList<>();
    for (EnvironmentFilterYaml entry : yaml.getEnvSelection()) {
      ChangeContext clonedContext = cloneFileChangeContext(changeContext, entry).build();

      environmentFilterYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.ENV_FILTER, entry.getFilterType().name());
      if (entry instanceof CustomEnvFilter.Yaml && appIds.size() != 1) {
        throw new InvalidRequestException(
            "Application filter should have exactly one app when environment filter type is CUSTOM");
      }
      // If Custom Env Filter, we need app Id to find the environments
      clonedContext.getEntityIdMap().put("appId", appIds.get(0));
      environmentFilters.add(environmentFilterYamlHandler.upsertFromYaml(clonedContext, changeSetContext));
    }

    bean.setApps(appIds);
    bean.setFilterType(yaml.getFilterType());
    bean.setEnvSelection(environmentFilters.get(0));
  }

  private List<String> getAppIds(String accountId, List<String> appNames) {
    List<Application> apps = new ArrayList<>();
    if (EmptyPredicate.isEmpty(appNames)) {
      throw new InvalidRequestException("Custom App filter requires 1 app");
    }
    for (String appName : appNames) {
      Application app = appService.getAppByName(accountId, appName);
      if (app == null) {
        throw new InvalidRequestException("Invalid App name: " + appName);
      }
      apps.add(app);
    }
    return apps.stream().map(Application::getUuid).collect(Collectors.toList());
  }
}
