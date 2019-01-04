package software.wings.expression;

import io.harness.expression.ExpressionEvaluator;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.ServiceTemplateService;

public class ManagerPreExecutionExpressionEvaluator extends ExpressionEvaluator {
  public ManagerPreExecutionExpressionEvaluator(ServiceTemplateService serviceTemplateService,
      ConfigService configService, String appId, String envId, String serviceTemplateId,
      ContainerDeploymentManagerHelper containerDeploymentHelper, String artifactStreamId) {
    addFunctor("configFile",
        ConfigFileFunctor.builder()
            .appId(appId)
            .envId(envId)
            .serviceTemplateId(serviceTemplateId)
            .configService(configService)
            .serviceTemplateService(serviceTemplateService)
            .build());

    addFunctor("dockerconfig",
        DockerConfigFunctor.builder()
            .appId(appId)
            .artifactStreamId(artifactStreamId)
            .containerDeploymentHelper(containerDeploymentHelper)
            .build());
  }
}
