package software.wings.service.impl.yaml.handler.governance;

import io.harness.governance.AllEnvFilter;
import io.harness.governance.AllEnvFilterYaml;
import io.harness.governance.EnvironmentFilter.EnvironmentFilterType;

import software.wings.beans.yaml.ChangeContext;

import java.util.List;

public class AllEnvFilterYamlHandler extends EnvironmentFilterYamlHandler<AllEnvFilterYaml, AllEnvFilter> {
  @Override
  public AllEnvFilterYaml toYaml(AllEnvFilter bean, String accountId) {
    return AllEnvFilterYaml.builder().environmentFilterType(bean.getFilterType()).build();
  }

  @Override
  public AllEnvFilter upsertFromYaml(
      ChangeContext<AllEnvFilterYaml> changeContext, List<ChangeContext> changeSetContext) {
    AllEnvFilter allEnvFilter = AllEnvFilter.builder().build();
    toBean(allEnvFilter, changeContext, changeSetContext);
    return allEnvFilter;
  }

  private void toBean(
      AllEnvFilter bean, ChangeContext<AllEnvFilterYaml> changeContext, List<ChangeContext> changeSetContext) {
    bean.setFilterType(EnvironmentFilterType.ALL);
  }

  @Override
  public Class getYamlClass() {
    return AllEnvFilterYaml.class;
  }
}
