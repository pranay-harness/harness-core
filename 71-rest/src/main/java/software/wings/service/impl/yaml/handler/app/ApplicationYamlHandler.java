package software.wings.service.impl.yaml.handler.app;

import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.EntityType.APPLICATION;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Application;
import software.wings.beans.Application.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;

import java.util.List;

/**
 * @author rktummala on 10/22/17
 */
@Singleton
public class ApplicationYamlHandler extends BaseYamlHandler<Application.Yaml, Application> {
  @Inject YamlHelper yamlHelper;
  @Inject AppService appService;

  @Override
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {
    Application application = get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    if (application != null) {
      appService.delete(application.getUuid(), changeContext.getChange().isSyncFromGit());
    }
  }

  @Override
  public Application.Yaml toYaml(Application application, String appId) {
    return Application.Yaml.builder()
        .type(APPLICATION.name())
        .description(application.getDescription())
        .harnessApiVersion(getHarnessApiVersion())
        .build();
  }

  @Override
  public Application upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    String accountId = changeContext.getChange().getAccountId();
    Yaml yaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    Application previous = get(accountId, yamlFilePath);

    String appName = yamlHelper.getAppName(yamlFilePath);
    Application current =
        anApplication().withAccountId(accountId).withName(appName).withDescription(yaml.getDescription()).build();

    current.setSyncFromGit(changeContext.getChange().isSyncFromGit());
    if (previous != null) {
      current.setUuid(previous.getUuid());
      return appService.update(current);
    } else {
      return appService.save(current);
    }
  }

  @Override
  public Class getYamlClass() {
    return Application.Yaml.class;
  }

  @Override
  public Application get(String accountId, String yamlFilePath) {
    return yamlHelper.getApp(accountId, yamlFilePath);
  }
}
