package software.wings.service.impl.yaml.handler.governance;

import io.harness.governance.AllProdEnvFilter;
import io.harness.governance.AllProdEnvFilterYaml;
import io.harness.governance.EnvironmentFilter.EnvironmentFilterType;

import software.wings.beans.yaml.ChangeContext;

import java.util.List;

public class AllProdEnvFilterYamlHandler extends EnvironmentFilterYamlHandler<AllProdEnvFilterYaml, AllProdEnvFilter> {
  @Override
  public AllProdEnvFilterYaml toYaml(AllProdEnvFilter bean, String accountId) {
    return AllProdEnvFilterYaml.builder().environmentFilterType(bean.getFilterType()).build();
  }

  @Override
  public AllProdEnvFilter upsertFromYaml(
      ChangeContext<AllProdEnvFilterYaml> changeContext, List<ChangeContext> changeSetContext) {
    AllProdEnvFilter customEnvFilter = AllProdEnvFilter.builder().build();
    toBean(customEnvFilter, changeContext, changeSetContext);
    return customEnvFilter;
  }

  private void toBean(
      AllProdEnvFilter bean, ChangeContext<AllProdEnvFilterYaml> changeContext, List<ChangeContext> changeSetContext) {
    bean.setFilterType(EnvironmentFilterType.ALL_PROD);
  }

  @Override
  public Class getYamlClass() {
    return AllProdEnvFilterYaml.class;
  }
}
