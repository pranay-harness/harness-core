package software.wings.service.impl.yaml.handler.app;

import static io.harness.annotations.dev.HarnessModule._870_CG_YAML;
import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.FeatureName.WEBHOOK_TRIGGER_AUTHORIZATION;

import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.EntityType.APPLICATION;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ff.FeatureFlagService;

import software.wings.beans.Application;
import software.wings.beans.Application.Yaml;
import software.wings.beans.EntityType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.yaml.gitSync.YamlGitConfig;
import software.wings.yaml.gitSync.YamlGitConfig.SyncMode;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 10/22/17
 */
@Singleton
@OwnedBy(CDC)
@TargetModule(_870_CG_YAML)
public class ApplicationYamlHandler extends BaseYamlHandler<Application.Yaml, Application> {
  @Inject YamlHelper yamlHelper;
  @Inject AppService appService;
  @Inject YamlGitService yamlGitService;
  @Inject FeatureFlagService featureFlagService;

  @Override
  public void delete(ChangeContext<Yaml> changeContext) {
    Application application = get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    if (application != null) {
      appService.delete(application.getUuid(), changeContext.getChange().isSyncFromGit());
      yamlGitService.delete(application.getAccountId(), application.getUuid(), EntityType.APPLICATION);
    }
  }

  @Override
  public Application.Yaml toYaml(Application application, String appId) {
    Yaml yaml = Yaml.builder()
                    .type(APPLICATION.name())
                    .description(application.getDescription())
                    .harnessApiVersion(getHarnessApiVersion())
                    .build();

    if (featureFlagService.isEnabled(WEBHOOK_TRIGGER_AUTHORIZATION, application.getAccountId())) {
      yaml.setIsManualTriggerAuthorized(application.getIsManualTriggerAuthorized());
    }
    updateYamlWithAdditionalInfo(application, appId, yaml);
    return yaml;
  }

  @Override
  public Application upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String accountId = changeContext.getChange().getAccountId();
    Yaml yaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    Application previous = get(accountId, yamlFilePath);

    String appName = yamlHelper.getAppName(yamlFilePath);
    Application current = anApplication().accountId(accountId).name(appName).description(yaml.getDescription()).build();

    if (featureFlagService.isEnabled(WEBHOOK_TRIGGER_AUTHORIZATION, accountId)) {
      if (yaml.getIsManualTriggerAuthorized() == null && previous != null
          && previous.getIsManualTriggerAuthorized() != null) {
        current.setIsManualTriggerAuthorized(false);
      } else {
        current.setIsManualTriggerAuthorized(yaml.getIsManualTriggerAuthorized());
      }
    }

    current.setSyncFromGit(changeContext.getChange().isSyncFromGit());

    Application updatedApplication;
    if (previous != null) {
      current.setUuid(previous.getUuid());
      current.setAppId(previous.getUuid());

      // Needs special handling for not modifying the yamlGitConfig for existing application
      YamlGitConfig yamlGitConfig = yamlGitService.get(accountId, current.getUuid(), EntityType.APPLICATION);
      current.setYamlGitConfig(yamlGitConfig);

      updatedApplication = appService.update(current);
    } else {
      YamlGitConfig yamlGitConfig = null;

      if (changeContext.getChange() instanceof GitFileChange) {
        yamlGitConfig = ((GitFileChange) changeContext.getChange()).getYamlGitConfig();
      }
      current.setYamlGitConfig(createAppYamlGitConfig(accountId, yamlGitConfig));

      updatedApplication = appService.save(current);
    }

    changeContext.setEntity(updatedApplication);
    return updatedApplication;
  }

  @Override
  public Class getYamlClass() {
    return Application.Yaml.class;
  }

  @Override
  public Application get(String accountId, String yamlFilePath) {
    return yamlHelper.getApp(accountId, yamlFilePath);
  }

  private YamlGitConfig createAppYamlGitConfig(String accountId, YamlGitConfig yamlGitConfig) {
    if (yamlGitConfig == null) {
      return null;
    }

    return YamlGitConfig.builder()
        .accountId(accountId)
        .gitConnectorId(yamlGitConfig.getGitConnectorId())
        .branchName(yamlGitConfig.getBranchName())
        .syncMode(SyncMode.BOTH)
        .enabled(true)
        .build();
  }
}
