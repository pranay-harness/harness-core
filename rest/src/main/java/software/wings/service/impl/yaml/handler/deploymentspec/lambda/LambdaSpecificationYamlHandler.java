package software.wings.service.impl.yaml.handler.deploymentspec.lambda;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import software.wings.api.DeploymentType;
import software.wings.beans.LambdaSpecification;
import software.wings.beans.LambdaSpecification.DefaultSpecification;
import software.wings.beans.LambdaSpecification.FunctionSpecification;
import software.wings.beans.LambdaSpecification.Yaml;
import software.wings.beans.ObjectType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.deploymentspec.DeploymentSpecificationYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.Validator;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
/**
 * @author rktummala on 11/15/17
 */
public class LambdaSpecificationYamlHandler extends DeploymentSpecificationYamlHandler<Yaml, LambdaSpecification> {
  @Inject private YamlHandlerFactory yamlHandlerFactory;
  @Inject private YamlHelper yamlHelper;
  @Inject private ServiceResourceService serviceResourceService;

  @Override
  public Yaml toYaml(LambdaSpecification lambdaSpecification, String appId) {
    // default specification
    BaseYamlHandler defaultSpecYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.DEFAULT_SPECIFICATION, ObjectType.DEFAULT_SPECIFICATION);
    DefaultSpecification defaultSpecification = lambdaSpecification.getDefaults();
    DefaultSpecification.Yaml defaultSpecYaml = null;
    if (defaultSpecification != null) {
      defaultSpecYaml = (DefaultSpecification.Yaml) defaultSpecYamlHandler.toYaml(defaultSpecification, appId);
    }

    // function specification
    List<FunctionSpecification.Yaml> functionSpecYamlList = Collections.emptyList();
    BaseYamlHandler functionSpecYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.FUNCTION_SPECIFICATION, ObjectType.FUNCTION_SPECIFICATION);
    List<FunctionSpecification> functionSpecificationList = lambdaSpecification.getFunctions();
    if (isNotEmpty(functionSpecificationList)) {
      functionSpecYamlList =
          functionSpecificationList.stream()
              .map(functionSpecification
                  -> (FunctionSpecification.Yaml) functionSpecYamlHandler.toYaml(functionSpecification, appId))
              .collect(Collectors.toList());
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
          (DefaultSpecificationYamlHandler) yamlHandlerFactory.getYamlHandler(
              YamlType.DEFAULT_SPECIFICATION, ObjectType.DEFAULT_SPECIFICATION);
      ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, defaultSpecYaml);
      defaultSpec = defaultSpecYamlHandler.upsertFromYaml(clonedContext.build(), changeSetContext);
    }

    // function specification
    List<FunctionSpecification> functionSpecList = Lists.newArrayList();
    if (isNotEmpty(yaml.getFunctions())) {
      BaseYamlHandler functionSpecYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.FUNCTION_SPECIFICATION, ObjectType.FUNCTION_SPECIFICATION);
      functionSpecList = yaml.getFunctions()
                             .stream()
                             .map(functionSpec -> {
                               try {
                                 ChangeContext.Builder clonedContext =
                                     cloneFileChangeContext(changeContext, functionSpec);
                                 return (FunctionSpecification) functionSpecYamlHandler.upsertFromYaml(
                                     clonedContext.build(), changeSetContext);
                               } catch (HarnessException e) {
                                 throw new WingsException(e);
                               }
                             })
                             .collect(Collectors.toList());
    }

    String appId =
        yamlHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    Validator.notNullCheck("Could not lookup app for the yaml file: " + changeContext.getChange().getFilePath(), appId);

    String serviceId = yamlHelper.getServiceId(appId, changeContext.getChange().getFilePath());
    Validator.notNullCheck(
        "Could not lookup service for the yaml file: " + changeContext.getChange().getFilePath(), serviceId);

    LambdaSpecification lambdaSpecification =
        LambdaSpecification.builder().defaults(defaultSpec).functions(functionSpecList).serviceId(serviceId).build();
    lambdaSpecification.setAppId(appId);
    return lambdaSpecification;
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    return true;
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public LambdaSpecification get(String accountId, String yamlFilePath) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("Could not lookup app for the yaml file: " + yamlFilePath, appId);

    String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);
    Validator.notNullCheck("Could not lookup service for the yaml file: " + yamlFilePath, serviceId);

    return serviceResourceService.getLambdaSpecification(appId, serviceId);
  }
}
