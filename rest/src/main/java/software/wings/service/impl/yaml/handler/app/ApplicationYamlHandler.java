package software.wings.service.impl.yaml.handler.app;

import static software.wings.beans.EntityType.APPLICATION;
import static software.wings.utils.Util.isEmpty;

import com.google.inject.Inject;

import software.wings.beans.Application;
import software.wings.beans.Application.Builder;
import software.wings.beans.Application.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.sync.YamlSyncHelper;
import software.wings.service.intfc.AppService;

import java.util.List;

/**
 * @author rktummala on 10/22/17
 */
public class ApplicationYamlHandler extends BaseYamlHandler<Application.Yaml, Application> {
  @Inject YamlSyncHelper yamlSyncHelper;
  @Inject AppService appService;

  @Override
  public Application.Yaml toYaml(Application application, String appId) {
    return Application.Yaml.Builder.anApplicationYaml()
        .withType(APPLICATION.name())
        .withName(application.getName())
        .withDescription(application.getDescription())
        .build();
  }

  @Override
  public Application updateFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    Application previous =
        yamlSyncHelper.getApp(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    Builder builder = previous.toBuilder();
    setWithYamlValues(builder, changeContext.getYaml());
    return appService.update(builder.build());
  }

  private void setWithYamlValues(Builder builder, Application.Yaml appYaml) {
    builder.withName(appYaml.getName()).withDescription(appYaml.getDescription()).build();
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Application.Yaml applicationYaml = changeContext.getYaml();
    return !(isEmpty(applicationYaml.getName()));
  }

  @Override
  public Application createFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    Builder builder = Builder.anApplication().withAccountId(changeContext.getChange().getAccountId());
    setWithYamlValues(builder, changeContext.getYaml());
    return appService.save(builder.build());
  }

  @Override
  public Class getYamlClass() {
    return Application.Yaml.class;
  }

  @Override
  public Application get(String accountId, String yamlFilePath) {
    return yamlSyncHelper.getApp(accountId, yamlFilePath);
  }
}
