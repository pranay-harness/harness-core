package software.wings.service.impl.yaml.handler.governance;

import io.harness.governance.AllNonProdEnvFilter;
import io.harness.governance.AllNonProdEnvFilterYaml;
import io.harness.governance.EnvironmentFilter.EnvironmentFilterType;

import software.wings.beans.yaml.ChangeContext;

import java.util.List;

public class AllNonProdEnvFilterYamlHandler
    extends EnvironmentFilterYamlHandler<AllNonProdEnvFilterYaml, AllNonProdEnvFilter> {
  @Override
  public AllNonProdEnvFilterYaml toYaml(AllNonProdEnvFilter bean, String accountId) {
    return AllNonProdEnvFilterYaml.builder().environmentFilterType(bean.getFilterType()).build();
  }

  @Override
  public AllNonProdEnvFilter upsertFromYaml(
      ChangeContext<AllNonProdEnvFilterYaml> changeContext, List<ChangeContext> changeSetContext) {
    AllNonProdEnvFilter customEnvFilter = AllNonProdEnvFilter.builder().build();
    toBean(customEnvFilter, changeContext, changeSetContext);
    return customEnvFilter;
  }

  private void toBean(AllNonProdEnvFilter bean, ChangeContext<AllNonProdEnvFilterYaml> changeContext,
      List<ChangeContext> changeSetContext) {
    bean.setFilterType(EnvironmentFilterType.ALL_NON_PROD);
  }

  @Override
  public Class getYamlClass() {
    return AllNonProdEnvFilterYaml.class;
  }
}
