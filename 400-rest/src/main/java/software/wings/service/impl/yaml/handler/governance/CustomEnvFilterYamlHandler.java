/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.governance;

import io.harness.exception.InvalidRequestException;
import io.harness.governance.CustomEnvFilter;
import io.harness.governance.EnvironmentFilter.EnvironmentFilterType;
import io.harness.validation.Validator;

import software.wings.beans.Environment;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.intfc.EnvironmentService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CustomEnvFilterYamlHandler extends EnvironmentFilterYamlHandler<CustomEnvFilter.Yaml, CustomEnvFilter> {
  @Inject private EnvironmentService environmentService;

  @Override
  public CustomEnvFilter.Yaml toYaml(CustomEnvFilter bean, String accountId) {
    Map<String, String> envIdName = environmentService.getEnvironmentsFromIds(accountId, bean.getEnvironments())
                                        .stream()
                                        .collect(Collectors.toMap(Environment::getUuid, Environment::getName));

    // Preserve the order of environments
    List<String> envNames = bean.getEnvironments().stream().map(envIdName::get).collect(Collectors.toList());
    return CustomEnvFilter.Yaml.builder().environments(envNames).environmentFilterType(bean.getFilterType()).build();
  }

  @Override
  public CustomEnvFilter upsertFromYaml(
      ChangeContext<CustomEnvFilter.Yaml> changeContext, List<ChangeContext> changeSetContext) {
    CustomEnvFilter customEnvFilter = CustomEnvFilter.builder().build();
    toBean(customEnvFilter, changeContext, changeSetContext);
    return customEnvFilter;
  }

  private void toBean(
      CustomEnvFilter bean, ChangeContext<CustomEnvFilter.Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String appId = changeContext.getEntityIdMap().get("appId");

    CustomEnvFilter.Yaml yaml = changeContext.getYaml();

    bean.setEnvironments(getEnvIds(yaml.getEnvironments(), appId));
    bean.setFilterType(EnvironmentFilterType.CUSTOM);
  }

  @Override
  public Class getYamlClass() {
    return null;
  }

  private List<String> getEnvIds(List<String> envNames, String appId) {
    Validator.notNullCheck("Environments are required for CUSTOM environment filter.", envNames);
    List<String> envIds = new ArrayList<>();
    for (String envName : envNames) {
      Environment environment = environmentService.getEnvironmentByName(appId, envName);
      if (environment == null) {
        throw new InvalidRequestException("Invalid Environment: " + envName);
      }
      envIds.add(environment.getUuid());
    }
    return envIds;
  }
}
