package software.wings.service.impl.yaml.handler.environment;

import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.utils.Util.isEmpty;

import com.google.inject.Inject;

import software.wings.beans.Environment;
import software.wings.beans.Environment.Builder;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.Environment.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.sync.YamlSyncHelper;
import software.wings.service.intfc.EnvironmentService;
import software.wings.utils.Validator;

import java.util.List;

/**
 * @author rktummala on 11/07/17
 */
public class EnvironmentYamlHandler extends BaseYamlHandler<Environment.Yaml, Environment> {
  @Inject YamlSyncHelper yamlSyncHelper;
  @Inject EnvironmentService environmentService;

  @Override
  public Environment.Yaml toYaml(Environment environment, String appId) {
    return Environment.Yaml.Builder.anYaml()
        .withType(ENVIRONMENT.name())
        .withName(environment.getName())
        .withDescription(environment.getDescription())
        .withEnvironmentType(environment.getEnvironmentType().name())
        .build();
  }

  @Override
  public Environment upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    ensureValidChange(changeContext, changeSetContext);
    String appId =
        yamlSyncHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    Validator.notNullCheck("appId null for given yaml file:" + changeContext.getChange().getFilePath(), appId);

    Environment current =
        Builder.anEnvironment()
            .withAppId(appId)
            .withName(changeContext.getYaml().getName())
            .withDescription(changeContext.getYaml().getDescription())
            .withEnvironmentType(EnvironmentType.valueOf(changeContext.getYaml().getEnvironmentType()))
            .build();

    Environment previous = yamlSyncHelper.getEnvironment(appId, changeContext.getChange().getFilePath());

    if (previous != null) {
      current.setUuid(previous.getUuid());
      return environmentService.update(current);
    } else {
      return environmentService.save(current);
    }
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Environment.Yaml envYaml = changeContext.getYaml();
    return !(isEmpty(envYaml.getName()));
  }

  @Override
  public Class getYamlClass() {
    return Environment.Yaml.class;
  }

  @Override
  public Environment get(String accountId, String yamlFilePath) {
    return yamlSyncHelper.getEnvironment(accountId, yamlFilePath);
  }

  @Override
  public Environment createFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return upsertFromYaml(changeContext, changeSetContext);
  }

  @Override
  public Environment updateFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return upsertFromYaml(changeContext, changeSetContext);
  }
}
