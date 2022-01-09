/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.deploymentspec.userdata;

import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import io.harness.exception.HarnessException;

import software.wings.api.DeploymentType;
import software.wings.beans.container.UserDataSpecification;
import software.wings.beans.container.UserDataSpecification.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.deploymentspec.DeploymentSpecificationYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ServiceResourceService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 1/7/17
 */
@Singleton
public class UserDataSpecificationYamlHandler extends DeploymentSpecificationYamlHandler<Yaml, UserDataSpecification> {
  @Inject private YamlHandlerFactory yamlHandlerFactory;
  @Inject private YamlHelper yamlHelper;
  @Inject private ServiceResourceService serviceResourceService;

  @Override
  public Yaml toYaml(UserDataSpecification bean, String appId) {
    return Yaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .type(DeploymentType.AMI.name())
        .data(bean.getData())
        .build();
  }

  @Override
  public UserDataSpecification upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    UserDataSpecification previous =
        get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    UserDataSpecification userDataSpecification = toBean(changeContext);
    userDataSpecification.setSyncFromGit(changeContext.getChange().isSyncFromGit());

    if (previous != null) {
      userDataSpecification.setUuid(previous.getUuid());
      return serviceResourceService.updateUserDataSpecification(userDataSpecification);
    } else {
      return serviceResourceService.createUserDataSpecification(userDataSpecification);
    }
  }

  private UserDataSpecification toBean(ChangeContext<Yaml> changeContext) throws HarnessException {
    Yaml yaml = changeContext.getYaml();

    String filePath = changeContext.getChange().getFilePath();
    String appId = yamlHelper.getAppId(changeContext.getChange().getAccountId(), filePath);
    notNullCheck("Could not lookup app for the yaml file: " + filePath, appId, USER);

    String serviceId = yamlHelper.getServiceId(appId, filePath);
    notNullCheck("Could not lookup service for the yaml file: " + filePath, serviceId, USER);

    UserDataSpecification userDataSpecification =
        UserDataSpecification.builder().data(yaml.getData()).serviceId(serviceId).build();
    userDataSpecification.setAppId(appId);
    return userDataSpecification;
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public UserDataSpecification get(String accountId, String yamlFilePath) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Could not lookup app for the yaml file: " + yamlFilePath, appId, USER);

    String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);
    notNullCheck("Could not lookup service for the yaml file: " + yamlFilePath, serviceId, USER);

    return serviceResourceService.getUserDataSpecification(appId, serviceId);
  }
}
