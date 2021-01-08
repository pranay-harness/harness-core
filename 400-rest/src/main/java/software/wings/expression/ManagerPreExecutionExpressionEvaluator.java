package software.wings.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.expression.ExpressionEvaluator;
import io.harness.expression.ExpressionFunctor;
import io.harness.ff.FeatureFlagService;

import software.wings.expression.NgSecretManagerFunctor.NgSecretManagerFunctorBuilder;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.NGSecretService;
import software.wings.service.intfc.security.SecretManager;

import java.util.Map;
import lombok.Value;

@OwnedBy(CDC)
@Value
public class ManagerPreExecutionExpressionEvaluator extends ExpressionEvaluator {
  private final ExpressionFunctor secretManagerFunctor;
  private final ExpressionFunctor ngSecretManagerFunctor;

  public ManagerPreExecutionExpressionEvaluator(SecretManagerMode mode, ServiceTemplateService serviceTemplateService,
      ConfigService configService, String appId, String envId, String serviceTemplateId,
      ArtifactCollectionUtils artifactCollectionUtils, String artifactStreamId, FeatureFlagService featureFlagService,
      ManagerDecryptionService managerDecryptionService, SecretManager secretManager, String accountId,
      String workflowExecutionId, int expressionFunctorToken, NGSecretService ngSecretService,
      Map<String, String> taskSetupAbstractions) {
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

    NgSecretManagerFunctorBuilder ngSecretManagerFunctorBuilder = NgSecretManagerFunctor.builder()
                                                                      .mode(mode)
                                                                      .accountId(accountId)
                                                                      .expressionFunctorToken(expressionFunctorToken)
                                                                      .secretManager(secretManager)
                                                                      .ngSecretService(ngSecretService);

    if (EmptyPredicate.isNotEmpty(taskSetupAbstractions)) {
      ngSecretManagerFunctorBuilder.orgId(taskSetupAbstractions.get("orgIdentifier"))
          .projectId(taskSetupAbstractions.get("projectIdentifier"));
    }

    ngSecretManagerFunctor = ngSecretManagerFunctorBuilder.build();
    addFunctor(NgSecretManagerFunctorInterface.FUNCTOR_NAME, ngSecretManagerFunctor);
  }
}
