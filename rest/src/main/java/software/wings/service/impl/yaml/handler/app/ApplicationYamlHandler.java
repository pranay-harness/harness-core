package software.wings.service.impl.yaml.handler.app;

import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.EntityType.APPLICATION;
import static software.wings.utils.Util.isEmpty;

import com.google.inject.Inject;

import software.wings.beans.Application;
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
  public Application upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    ensureValidChange(changeContext, changeSetContext);

    Application current = anApplication()
                              .withAccountId(changeContext.getChange().getAccountId())
                              .withName(changeContext.getYaml().getName())
                              .withDescription(changeContext.getYaml().getDescription())
                              .build();
    Application previous = get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());

    if (previous != null) {
      current.setUuid(previous.getUuid());
      return appService.update(current);
    } else {
      return appService.save(current);
    }
  }

  @Override
  public Application updateFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return upsertFromYaml(changeContext, changeSetContext);
  }

  @Override
  public Application createFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return upsertFromYaml(changeContext, changeSetContext);
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Application.Yaml applicationYaml = changeContext.getYaml();
    return !(isEmpty(applicationYaml.getName()));
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
