/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.governance;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.HarnessException;
import io.harness.exception.WingsException;
import io.harness.exception.YamlException;
import io.harness.governance.TimeRangeBasedFreezeConfig;

import software.wings.beans.governance.GovernanceConfig;
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

public class GovernanceConfigYamlHandler extends BaseYamlHandler<GovernanceConfig.Yaml, GovernanceConfig> {
  private static final String TIME_RANGE_BASED_YAML_TYPE = "TIME_RANGE_BASED_FREEZE_CONFIG";
  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject GovernanceConfigService governanceConfigService;
  @Override
  public void delete(ChangeContext<GovernanceConfig.Yaml> changeContext) throws HarnessException {}

  @Override
  public GovernanceConfig.Yaml toYaml(GovernanceConfig bean, String appId) {
    String accountId = bean.getAccountId();

    TimeRangeBasedFreezeConfigYamlHandler timeRangeBasedFreezeConfigYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.GOVERNANCE_FREEZE_CONFIG, TIME_RANGE_BASED_YAML_TYPE);

    List<TimeRangeBasedFreezeConfig.Yaml> timeRangeBasedFreezeConfigYaml =
        bean.getTimeRangeBasedFreezeConfigs()
            .stream()
            .map(timeRangeBasedFreezeConfig -> {
              return timeRangeBasedFreezeConfigYamlHandler.toYaml(timeRangeBasedFreezeConfig, accountId);
            })
            .collect(Collectors.toList());

    return GovernanceConfig.Yaml.builder()
        .type(YamlType.GOVERNANCE_CONFIG.name())
        .harnessApiVersion(getHarnessApiVersion())
        .disableAllDeployments(bean.isDeploymentFreeze())
        .timeRangeBasedFreezeConfigs(timeRangeBasedFreezeConfigYaml)
        .build();
  }

  @Override
  public GovernanceConfig upsertFromYaml(ChangeContext<GovernanceConfig.Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String accountId = changeContext.getChange().getAccountId();

    // recreate the object
    GovernanceConfig governanceConfig = GovernanceConfig.builder().build();
    governanceConfig.setSyncFromGit(changeContext.getChange().isSyncFromGit());

    toBean(governanceConfig, changeContext, changeSetContext);
    return governanceConfigService.upsert(accountId, governanceConfig);
  }

  @Override
  public Class getYamlClass() {
    return GovernanceConfig.Yaml.class;
  }

  @Override
  public GovernanceConfig get(String accountId, String yamlFilePath) {
    return governanceConfigService.get(accountId);
  }

  private void toBean(
      GovernanceConfig bean, ChangeContext<GovernanceConfig.Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String accountId = changeContext.getChange().getAccountId();
    String entityId = "";

    if (!(changeContext.getChange() instanceof GitFileChange)) {
      throw new YamlException("Error while determining Id for GovernanceConfig", WingsException.USER);
    }

    entityId = ((GitFileChange) changeContext.getChange()).getEntityId();

    GovernanceConfig.Yaml yaml = changeContext.getYaml();

    TimeRangeBasedFreezeConfigYamlHandler timeRangeBasedFreezeConfigYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.GOVERNANCE_FREEZE_CONFIG, TIME_RANGE_BASED_YAML_TYPE);

    List<TimeRangeBasedFreezeConfig> timeRangeBasedFreezeConfigs = new ArrayList<>();
    if (EmptyPredicate.isNotEmpty(yaml.getTimeRangeBasedFreezeConfigs())) {
      for (TimeRangeBasedFreezeConfig.Yaml entry : yaml.getTimeRangeBasedFreezeConfigs()) {
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
