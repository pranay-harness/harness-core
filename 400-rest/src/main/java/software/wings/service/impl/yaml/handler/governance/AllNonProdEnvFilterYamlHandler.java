package software.wings.service.impl.yaml.handler.governance;

import io.harness.governance.AllNonProdEnvFilter;
import io.harness.governance.EnvironmentFilter.EnvironmentFilterType;

import software.wings.beans.yaml.ChangeContext;

import java.util.List;

public class AllNonProdEnvFilterYamlHandler
    extends EnvironmentFilterYamlHandler<AllNonProdEnvFilter.Yaml, AllNonProdEnvFilter> {
  @Override
  public AllNonProdEnvFilter.Yaml toYaml(AllNonProdEnvFilter bean, String accountId) {
    return AllNonProdEnvFilter.Yaml.builder().environmentFilterType(bean.getFilterType()).build();
  }

  @Override
  public AllNonProdEnvFilter upsertFromYaml(
      ChangeContext<AllNonProdEnvFilter.Yaml> changeContext, List<ChangeContext> changeSetContext) {
    AllNonProdEnvFilter customEnvFilter = AllNonProdEnvFilter.builder().build();
    toBean(customEnvFilter, changeContext, changeSetContext);
    return customEnvFilter;
  }

  private void toBean(AllNonProdEnvFilter bean, ChangeContext<AllNonProdEnvFilter.Yaml> changeContext,
      List<ChangeContext> changeSetContext) {
    bean.setFilterType(EnvironmentFilterType.ALL_NON_PROD);
  }

  @Override
  public Class getYamlClass() {
    return AllNonProdEnvFilter.Yaml.class;
  }
}
