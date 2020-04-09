package software.wings.expression;

import io.harness.expression.ExpressionEvaluator;
import io.harness.expression.ExpressionFunctor;
import lombok.Value;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;

@Value
public class ManagerPreExecutionExpressionEvaluator extends ExpressionEvaluator {
  private final ExpressionFunctor secretManagerFunctor;

  public ManagerPreExecutionExpressionEvaluator(SecretManagerFunctor.Mode mode,
      ServiceTemplateService serviceTemplateService, ConfigService configService, String appId, String envId,
      String serviceTemplateId, ArtifactCollectionUtils artifactCollectionUtils, String artifactStreamId,
      FeatureFlagService featureFlagService, ManagerDecryptionService managerDecryptionService,
      SecretManager secretManager, String accountId, String workflowExecutionId, int expressionFunctorToken) {
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
            .artifactCollectionUtils(artifactCollectionUtils)
            .build());

    secretManagerFunctor = SecretManagerFunctor.builder()
                               .mode(mode)
                               .featureFlagService(featureFlagService)
                               .managerDecryptionService(managerDecryptionService)
                               .secretManager(secretManager)
                               .accountId(accountId)
                               .appId(appId)
                               .envId(envId)
                               .workflowExecutionId(workflowExecutionId)
                               .expressionFunctorToken(expressionFunctorToken)
                               .build();
    addFunctor(SecretManagerFunctorInterface.FUNCTOR_NAME, secretManagerFunctor);
  }
}
