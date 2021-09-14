/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
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

public class CustomEnvFilterYamlHandler extends EnvironmentFilterYamlHandler<CustomEnvFilter.Yaml, CustomEnvFilter> {
  @Inject private EnvironmentService environmentService;

  @Override
  public CustomEnvFilter.Yaml toYaml(CustomEnvFilter bean, String accountId) {
    List<String> envNames = environmentService.getNames(accountId, bean.getEnvironments());

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
