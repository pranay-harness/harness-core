package software.wings.service.impl.yaml;

import static software.wings.beans.yaml.YamlConstants.YAML_EXTENSION;

import software.wings.beans.Application;
import software.wings.beans.RestResponse;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.yaml.AppYamlResourceService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.YamlHistoryService;
import software.wings.service.intfc.yaml.sync.YamlSyncService;
import software.wings.yaml.YamlHelper;
import software.wings.yaml.YamlPayload;

import javax.inject.Inject;

public class AppYamlResourceServiceImpl implements AppYamlResourceService {
  @Inject private AppService appService;
  @Inject private YamlHandlerFactory yamlHandlerFactory;
  @Inject private YamlSyncService yamlSyncService;
  @Inject private YamlHistoryService yamlHistoryService;
  @Inject private YamlGitService yamlGitSyncService;

  /**
   * Gets the yaml version of an app by appId
   *
   * @param appId  the app id
   * @return the rest response
   */
  public RestResponse<YamlPayload> getApp(String appId) {
    Application app = appService.get(appId);

    BaseYamlHandler yamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.APPLICATION, null);
    Application.Yaml applicationYaml = (Application.Yaml) yamlHandler.toYaml(app, appId);

    return YamlHelper.getYamlRestResponse(
        yamlGitSyncService, appId, app.getAccountId(), applicationYaml, app.getName() + YAML_EXTENSION);
  }

  /**
   * Update an app that is sent as Yaml (in a JSON "wrapper")
   *
   * @param appId  the app id
   * @param yamlPayload the yaml version of app
   * @return the rest response
   */
  public RestResponse<Application> updateApp(String appId, YamlPayload yamlPayload, boolean deleteEnabled) {
    String accountId = appService.getAccountIdByAppId(appId);
    return yamlSyncService.update(yamlPayload, accountId);
  }
}
