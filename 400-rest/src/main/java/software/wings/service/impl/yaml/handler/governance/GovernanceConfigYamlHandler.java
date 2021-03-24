package software.wings.service.impl.yaml.handler.governance;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.HarnessException;
import io.harness.exception.WingsException;
import io.harness.exception.YamlException;
import io.harness.governance.TimeRangeBasedFreezeConfig;
import io.harness.governance.TimeRangeBasedFreezeConfigYaml;

import software.wings.beans.governance.GovernanceConfig;
import software.wings.beans.governance.GovernanceConfigYaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.intfc.compliance.GovernanceConfigService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GovernanceConfigYamlHandler extends BaseYamlHandler<GovernanceConfigYaml, GovernanceConfig> {
  private static final String TIME_RANGE_BASED_YAML_TYPE = "TIME_RANGE_BASED_FREEZE_CONFIG";
  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject GovernanceConfigService governanceConfigService;
  @Override
  public void delete(ChangeContext<GovernanceConfigYaml> changeContext) throws HarnessException {}

  @Override
  public GovernanceConfigYaml toYaml(GovernanceConfig bean, String appId) {
    String accountId = bean.getAccountId();

    TimeRangeBasedFreezeConfigYamlHandler timeRangeBasedFreezeConfigYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.GOVERNANCE_FREEZE_CONFIG, TIME_RANGE_BASED_YAML_TYPE);

    List<TimeRangeBasedFreezeConfigYaml> timeRangeBasedFreezeConfigYaml =
        bean.getTimeRangeBasedFreezeConfigs()
            .stream()
            .map(timeRangeBasedFreezeConfig -> {
              return timeRangeBasedFreezeConfigYamlHandler.toYaml(timeRangeBasedFreezeConfig, accountId);
            })
            .collect(Collectors.toList());

    return GovernanceConfigYaml.builder()
        .type(YamlType.GOVERNANCE_CONFIG.name())
        .harnessApiVersion(getHarnessApiVersion())
        .disableAllDeployments(bean.isDeploymentFreeze())
        .timeRangeBasedFreezeConfigs(timeRangeBasedFreezeConfigYaml)
        .build();
  }

  @Override
  public GovernanceConfig upsertFromYaml(
      ChangeContext<GovernanceConfigYaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    String accountId = changeContext.getChange().getAccountId();

    // recreate the object
    GovernanceConfig governanceConfig = GovernanceConfig.builder().build();
    governanceConfig.setSyncFromGit(changeContext.getChange().isSyncFromGit());

    toBean(governanceConfig, changeContext, changeSetContext);
    return governanceConfigService.upsert(accountId, governanceConfig);
  }

  @Override
  public Class getYamlClass() {
    return GovernanceConfigYaml.class;
  }

  @Override
  public GovernanceConfig get(String accountId, String yamlFilePath) {
    return governanceConfigService.get(accountId);
  }

  private void toBean(
      GovernanceConfig bean, ChangeContext<GovernanceConfigYaml> changeContext, List<ChangeContext> changeSetContext) {
    String accountId = changeContext.getChange().getAccountId();
    String entityId = "";

    if (!(changeContext.getChange() instanceof GitFileChange)) {
      throw new YamlException("Error while determining Id for GovernanceConfig", WingsException.USER);
    }

    entityId = ((GitFileChange) changeContext.getChange()).getEntityId();

    GovernanceConfigYaml yaml = changeContext.getYaml();

    TimeRangeBasedFreezeConfigYamlHandler timeRangeBasedFreezeConfigYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.GOVERNANCE_FREEZE_CONFIG, TIME_RANGE_BASED_YAML_TYPE);

    List<TimeRangeBasedFreezeConfig> timeRangeBasedFreezeConfigs = new ArrayList<>();
    if (EmptyPredicate.isNotEmpty(yaml.getTimeRangeBasedFreezeConfigs())) {
      for (TimeRangeBasedFreezeConfigYaml entry : yaml.getTimeRangeBasedFreezeConfigs()) {
        ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, entry);
        timeRangeBasedFreezeConfigs.add(
            timeRangeBasedFreezeConfigYamlHandler.upsertFromYaml(clonedContext.build(), changeSetContext));
      }
    }

    bean.setUuid(entityId);
    bean.setTimeRangeBasedFreezeConfigs(timeRangeBasedFreezeConfigs);
    bean.setAccountId(accountId);
    bean.setDeploymentFreeze(yaml.isDisableAllDeployments());
  }
}
