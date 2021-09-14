/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.handler.governance;

import io.harness.governance.AllProdEnvFilter;
import io.harness.governance.EnvironmentFilter.EnvironmentFilterType;

import software.wings.beans.yaml.ChangeContext;

import java.util.List;

public class AllProdEnvFilterYamlHandler extends EnvironmentFilterYamlHandler<AllProdEnvFilter.Yaml, AllProdEnvFilter> {
  @Override
  public AllProdEnvFilter.Yaml toYaml(AllProdEnvFilter bean, String accountId) {
    return AllProdEnvFilter.Yaml.builder().environmentFilterType(bean.getFilterType()).build();
  }

  @Override
  public AllProdEnvFilter upsertFromYaml(
      ChangeContext<AllProdEnvFilter.Yaml> changeContext, List<ChangeContext> changeSetContext) {
    AllProdEnvFilter customEnvFilter = AllProdEnvFilter.builder().build();
    toBean(customEnvFilter, changeContext, changeSetContext);
    return customEnvFilter;
  }

  private void toBean(
      AllProdEnvFilter bean, ChangeContext<AllProdEnvFilter.Yaml> changeContext, List<ChangeContext> changeSetContext) {
    bean.setFilterType(EnvironmentFilterType.ALL_PROD);
  }

  @Override
  public Class getYamlClass() {
    return AllProdEnvFilter.Yaml.class;
  }
}
