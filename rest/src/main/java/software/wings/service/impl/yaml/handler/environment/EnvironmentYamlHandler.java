package software.wings.service.impl.yaml.handler.environment;

import static software.wings.beans.EntityType.ENVIRONMENT;

import com.google.inject.Inject;

import software.wings.beans.Environment;
import software.wings.beans.Environment.Builder;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.Environment.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.EnvironmentService;
import software.wings.utils.Validator;

import java.util.List;

/**
 * @author rktummala on 11/07/17
 */
public class EnvironmentYamlHandler extends BaseYamlHandler<Environment.Yaml, Environment> {
  @Inject YamlHelper yamlHelper;
  @Inject EnvironmentService environmentService;

  @Override
  public Environment.Yaml toYaml(Environment environment, String appId) {
    return Environment.Yaml.builder()
        .type(ENVIRONMENT.name())
        .description(environment.getDescription())
        .environmentType(environment.getEnvironmentType().name())
        .build();
  }

  @Override
  public Environment upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    ensureValidChange(changeContext, changeSetContext);
    String appId =
        yamlHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    Validator.notNullCheck("appId null for given yaml file:" + changeContext.getChange().getFilePath(), appId);
    Yaml yaml = changeContext.getYaml();
    String environmentName = yamlHelper.getEnvironmentName(changeContext.getChange().getFilePath());
    Environment current = Builder.anEnvironment()
                              .withAppId(appId)
                              .withName(environmentName)
                              .withDescription(yaml.getDescription())
                              .withEnvironmentType(EnvironmentType.valueOf(yaml.getEnvironmentType()))
                              .build();

    Environment previous = yamlHelper.getEnvironment(appId, changeContext.getChange().getFilePath());

    if (previous != null) {
      current.setUuid(previous.getUuid());
      return environmentService.update(current);
    } else {
      return environmentService.save(current);
    }
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    return true;
  }

  @Override
  public Class getYamlClass() {
    return Environment.Yaml.class;
  }

  @Override
  public Environment get(String accountId, String yamlFilePath) {
    return yamlHelper.getEnvironment(accountId, yamlFilePath);
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {
    Environment environment = get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    if (environment != null) {
      environmentService.delete(environment.getAppId(), environment.getUuid());
    }
  }
}
