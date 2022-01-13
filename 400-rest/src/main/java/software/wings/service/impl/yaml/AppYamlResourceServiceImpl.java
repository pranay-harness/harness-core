/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml;

import static software.wings.beans.yaml.YamlConstants.YAML_EXTENSION;

import io.harness.rest.RestResponse;

import software.wings.beans.Application;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.app.ApplicationYamlHandler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.yaml.AppYamlResourceService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.YamlHistoryService;
import software.wings.service.intfc.yaml.sync.YamlService;
import software.wings.yaml.YamlHelper;
import software.wings.yaml.YamlPayload;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AppYamlResourceServiceImpl implements AppYamlResourceService {
  @Inject private AppService appService;
  @Inject private YamlHandlerFactory yamlHandlerFactory;
  @Inject private YamlService yamlSyncService;
  @Inject private YamlHistoryService yamlHistoryService;
  @Inject private YamlGitService yamlGitSyncService;

  /**
   * Gets the yaml version of an app by appId
   *
   * @param appId  the app id
   * @return the rest response
   */
  @Override
  public RestResponse<YamlPayload> getApp(String appId) {
    Application app = appService.get(appId);

    ApplicationYamlHandler yamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.APPLICATION);
    Application.Yaml applicationYaml = yamlHandler.toYaml(app, appId);

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
  @Override
  public RestResponse<Application> updateApp(String appId, YamlPayload yamlPayload, boolean deleteEnabled) {
    String accountId = appService.getAccountIdByAppId(appId);
    return yamlSyncService.update(yamlPayload, accountId, appId);
  }
}
