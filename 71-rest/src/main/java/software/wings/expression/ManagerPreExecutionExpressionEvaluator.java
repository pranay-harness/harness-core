package software.wings.expression;

import io.harness.expression.ExpressionEvaluator;
import io.harness.expression.ExpressionFunctor;
import lombok.Value;
import software.wings.service.impl.artifact.ArtifactCollectionUtil;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;

@Value
public class ManagerPreExecutionExpressionEvaluator extends ExpressionEvaluator {
  private ExpressionFunctor secretManagerFunctor;

  public ManagerPreExecutionExpressionEvaluator(ServiceTemplateService serviceTemplateService,
      ConfigService configService, String appId, String envId, String serviceTemplateId,
      ArtifactCollectionUtil artifactCollectionUtil, String artifactStreamId,
      ManagerDecryptionService managerDecryptionService, SecretManager secretManager, String accountId,
      String workflowExecutionId) {
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

    secretManagerFunctor = SecretManagerFunctor.builder()
                               .managerDecryptionService(managerDecryptionService)
                               .secretManager(secretManager)
                               .accountId(accountId)
                               .appId(appId)
                               .workflowExecutionId(workflowExecutionId)
                               .build();
    addFunctor("secretManager", secretManagerFunctor);
  }
}
