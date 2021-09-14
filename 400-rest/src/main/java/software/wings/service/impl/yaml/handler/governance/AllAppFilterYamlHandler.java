/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.handler.governance;

import io.harness.exception.InvalidRequestException;
import io.harness.governance.AllAppFilter;
import io.harness.governance.BlackoutWindowFilterType;
import io.harness.governance.EnvironmentFilter;
import io.harness.governance.EnvironmentFilterYaml;

import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.intfc.AppService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AllAppFilterYamlHandler extends ApplicationFilterYamlHandler<AllAppFilter.Yaml, AllAppFilter> {
  @Inject private AppService appService;

  @Inject YamlHandlerFactory yamlHandlerFactory;

  @Override
  public AllAppFilter.Yaml toYaml(AllAppFilter bean, String accountId) {
    EnvironmentFilterYamlHandler environmentFilterYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.ENV_FILTER, bean.getEnvSelection().getFilterType().name());

    // envSelection is made a List to make Yaml cleanup work YamlUtils.cleanUpDoubleExclamationLines
    return AllAppFilter.Yaml.builder()
        .envSelection(Arrays.asList(environmentFilterYamlHandler.toYaml(bean.getEnvSelection(), accountId)))
        .filterType(BlackoutWindowFilterType.ALL)
        .build();
  }

  @Override
  public AllAppFilter upsertFromYaml(
      ChangeContext<AllAppFilter.Yaml> changeContext, List<ChangeContext> changeSetContext) {
    AllAppFilter appAppFilter = AllAppFilter.builder().build();
    toBean(appAppFilter, changeContext, changeSetContext);
    return appAppFilter;
  }

  @Override
  public Class getYamlClass() {
    return AllAppFilter.Yaml.class;
  }

  private void toBean(
      AllAppFilter bean, ChangeContext<AllAppFilter.Yaml> changeContext, List<ChangeContext> changeSetContext) {
    AllAppFilter.Yaml yaml = changeContext.getYaml();

    EnvironmentFilterYamlHandler environmentFilterYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.ENV_FILTER, yaml.getEnvSelection().get(0).getFilterType().name());

    if (environmentFilterYamlHandler instanceof CustomEnvFilterYamlHandler) {
      throw new InvalidRequestException("CUSTOM Environments can be selected with only selecting 1 app");
    }

    List<EnvironmentFilter> environmentFilters = new ArrayList<>();
    for (EnvironmentFilterYaml entry : yaml.getEnvSelection()) {
      ChangeContext clonedContext = cloneFileChangeContext(changeContext, entry).build();
      environmentFilters.add(environmentFilterYamlHandler.upsertFromYaml(clonedContext, changeSetContext));
    }

    bean.setFilterType(yaml.getFilterType());
    bean.setEnvSelection(environmentFilters.get(0));
  }
}
