package software.wings.service.impl.yaml.handler.governance;

import io.harness.exception.InvalidRequestException;
import io.harness.governance.CustomEnvFilter;
import io.harness.governance.CustomEnvFilterYaml;
import io.harness.governance.EnvironmentFilter.EnvironmentFilterType;
import io.harness.validation.Validator;

import software.wings.beans.Environment;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.intfc.EnvironmentService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class CustomEnvFilterYamlHandler extends EnvironmentFilterYamlHandler<CustomEnvFilterYaml, CustomEnvFilter> {
  @Inject private EnvironmentService environmentService;

  @Override
  public CustomEnvFilterYaml toYaml(CustomEnvFilter bean, String accountId) {
    List<String> envNames = environmentService.getNames(accountId, bean.getEnvironments());

    return CustomEnvFilterYaml.builder().environments(envNames).environmentFilterType(bean.getFilterType()).build();
  }

  @Override
  public CustomEnvFilter upsertFromYaml(
      ChangeContext<CustomEnvFilterYaml> changeContext, List<ChangeContext> changeSetContext) {
    CustomEnvFilter customEnvFilter = CustomEnvFilter.builder().build();
    toBean(customEnvFilter, changeContext, changeSetContext);
    return customEnvFilter;
  }

  private void toBean(
      CustomEnvFilter bean, ChangeContext<CustomEnvFilterYaml> changeContext, List<ChangeContext> changeSetContext) {
    String appId = changeContext.getEntityIdMap().get("appId");

    CustomEnvFilterYaml yaml = changeContext.getYaml();

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
