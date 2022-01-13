/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.deploymentspec.lambda;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import static java.util.stream.Collectors.toList;

import io.harness.exception.HarnessException;
import io.harness.exception.WingsException;

import software.wings.api.DeploymentType;
import software.wings.beans.LambdaSpecification;
import software.wings.beans.LambdaSpecification.DefaultSpecification;
import software.wings.beans.LambdaSpecification.FunctionSpecification;
import software.wings.beans.LambdaSpecification.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.deploymentspec.DeploymentSpecificationYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ServiceResourceService;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;
/**
 * @author rktummala on 11/15/17
 */
@Singleton
public class LambdaSpecificationYamlHandler extends DeploymentSpecificationYamlHandler<Yaml, LambdaSpecification> {
  @Inject private YamlHandlerFactory yamlHandlerFactory;
  @Inject private YamlHelper yamlHelper;
  @Inject private ServiceResourceService serviceResourceService;

  @Override
  public Yaml toYaml(LambdaSpecification lambdaSpecification, String appId) {
    // default specification
    DefaultSpecificationYamlHandler defaultSpecYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.DEFAULT_SPECIFICATION);
    DefaultSpecification defaultSpecification = lambdaSpecification.getDefaults();
    DefaultSpecification.Yaml defaultSpecYaml = null;
    if (defaultSpecification != null) {
      defaultSpecYaml = defaultSpecYamlHandler.toYaml(defaultSpecification, appId);
    }

    // function specification
    List<FunctionSpecification.Yaml> functionSpecYamlList = Collections.emptyList();
    FunctionSpecificationYamlHandler functionSpecYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.FUNCTION_SPECIFICATION);
    List<FunctionSpecification> functionSpecificationList = lambdaSpecification.getFunctions();
    if (isNotEmpty(functionSpecificationList)) {
      functionSpecYamlList =
          functionSpecificationList.stream()
              .map(functionSpecification -> functionSpecYamlHandler.toYaml(functionSpecification, appId))
              .collect(toList());
    }

    return Yaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .type(DeploymentType.AWS_LAMBDA.name())
        .functions(functionSpecYamlList)
        .defaults(defaultSpecYaml)
        .build();
  }

  @Override
  public LambdaSpecification upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    LambdaSpecification previous =
        get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    LambdaSpecification lambdaSpecification = toBean(changeContext, changeSetContext);
    lambdaSpecification.setSyncFromGit(changeContext.getChange().isSyncFromGit());

    if (previous != null) {
      lambdaSpecification.setUuid(previous.getUuid());
      return serviceResourceService.updateLambdaSpecification(lambdaSpecification);
    } else {
      return serviceResourceService.createLambdaSpecification(lambdaSpecification);
    }
  }

  private LambdaSpecification toBean(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    Yaml yaml = changeContext.getYaml();

    // default specification
    DefaultSpecification defaultSpec = null;
    DefaultSpecification.Yaml defaultSpecYaml = yaml.getDefaults();
    if (defaultSpecYaml != null) {
      DefaultSpecificationYamlHandler defaultSpecYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.DEFAULT_SPECIFICATION);
      ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, defaultSpecYaml);
      defaultSpec = defaultSpecYamlHandler.upsertFromYaml(clonedContext.build(), changeSetContext);
    }

    // function specification
    List<FunctionSpecification> functionSpecList = Lists.newArrayList();
    if (isNotEmpty(yaml.getFunctions())) {
      FunctionSpecificationYamlHandler functionSpecYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.FUNCTION_SPECIFICATION);
      functionSpecList = yaml.getFunctions()
                             .stream()
                             .map(functionSpec -> {
                               try {
                                 ChangeContext.Builder clonedContext =
                                     cloneFileChangeContext(changeContext, functionSpec);
                                 return functionSpecYamlHandler.upsertFromYaml(clonedContext.build(), changeSetContext);
                               } catch (HarnessException e) {
                                 throw new WingsException(e);
                               }
                             })
                             .collect(toList());
    }

    String appId =
        yamlHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    notNullCheck("Could not lookup app for the yaml file: " + changeContext.getChange().getFilePath(), appId, USER);

    String serviceId = yamlHelper.getServiceId(appId, changeContext.getChange().getFilePath());
    notNullCheck(
        "Could not lookup service for the yaml file: " + changeContext.getChange().getFilePath(), serviceId, USER);

    LambdaSpecification lambdaSpecification =
        LambdaSpecification.builder().defaults(defaultSpec).functions(functionSpecList).serviceId(serviceId).build();
    lambdaSpecification.setAppId(appId);
    return lambdaSpecification;
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public LambdaSpecification get(String accountId, String yamlFilePath) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Could not lookup app for the yaml file: " + yamlFilePath, appId, USER);

    String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);
    notNullCheck("Could not lookup service for the yaml file: " + yamlFilePath, serviceId, USER);

    return serviceResourceService.getLambdaSpecification(appId, serviceId);
  }
}
