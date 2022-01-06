/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.governance;

import io.harness.governance.AllEnvFilter;
import io.harness.governance.EnvironmentFilter.EnvironmentFilterType;

import software.wings.beans.yaml.ChangeContext;

import java.util.List;

public class AllEnvFilterYamlHandler extends EnvironmentFilterYamlHandler<AllEnvFilter.Yaml, AllEnvFilter> {
  @Override
  public AllEnvFilter.Yaml toYaml(AllEnvFilter bean, String accountId) {
    return AllEnvFilter.Yaml.builder().environmentFilterType(bean.getFilterType()).build();
  }

  @Override
  public AllEnvFilter upsertFromYaml(
      ChangeContext<AllEnvFilter.Yaml> changeContext, List<ChangeContext> changeSetContext) {
    AllEnvFilter allEnvFilter = AllEnvFilter.builder().build();
    toBean(allEnvFilter, changeContext, changeSetContext);
    return allEnvFilter;
  }

  private void toBean(
      AllEnvFilter bean, ChangeContext<AllEnvFilter.Yaml> changeContext, List<ChangeContext> changeSetContext) {
    bean.setFilterType(EnvironmentFilterType.ALL);
  }

  @Override
  public Class getYamlClass() {
    return AllEnvFilter.Yaml.class;
  }
}
