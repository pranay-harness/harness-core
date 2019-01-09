package software.wings.expression;

import io.harness.expression.ExpressionEvaluator;
import software.wings.service.impl.artifact.ArtifactCollectionUtil;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.ServiceTemplateService;

public class ManagerPreExecutionExpressionEvaluator extends ExpressionEvaluator {
  public ManagerPreExecutionExpressionEvaluator(ServiceTemplateService serviceTemplateService,
      ConfigService configService, String appId, String envId, String serviceTemplateId,
      ArtifactCollectionUtil artifactCollectionUtil, String artifactStreamId) {
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
            .artifactCollectionUtil(artifactCollectionUtil)
            .build());
  }
}
