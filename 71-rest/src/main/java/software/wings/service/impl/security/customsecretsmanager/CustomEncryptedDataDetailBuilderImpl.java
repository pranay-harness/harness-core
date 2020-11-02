package software.wings.service.impl.security.customsecretsmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedDataParams;
import io.harness.security.encryption.EncryptedRecordData;
import lombok.extern.slf4j.Slf4j;
import software.wings.expression.SecretFunctor;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.security.CustomEncryptedDataDetailBuilder;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@OwnedBy(PL)
@Slf4j
public class CustomEncryptedDataDetailBuilderImpl implements CustomEncryptedDataDetailBuilder {
  private CustomSecretsManagerConnectorHelper customSecretsManagerConnectorHelper;
  private SecretManager secretManager;
  private ManagerDecryptionService managerDecryptionService;
  private ExpressionEvaluator expressionEvaluator;
  private FeatureFlagService featureFlagService;

  @Inject
  public CustomEncryptedDataDetailBuilderImpl(CustomSecretsManagerConnectorHelper customSecretsManagerConnectorHelper,
      SecretManager secretManager, ManagerDecryptionService managerDecryptionService,
      ExpressionEvaluator expressionEvaluator, FeatureFlagService featureFlagService) {
    this.customSecretsManagerConnectorHelper = customSecretsManagerConnectorHelper;
    this.secretManager = secretManager;
    this.managerDecryptionService = managerDecryptionService;
    this.expressionEvaluator = expressionEvaluator;
    this.featureFlagService = featureFlagService;
  }

  public EncryptedDataDetail buildEncryptedDataDetail(
      EncryptedData encryptedData, CustomSecretsManagerConfig customSecretsManagerConfig) {
    CustomSecretsManagerValidationUtils.validateVariables(customSecretsManagerConfig, encryptedData.getParameters());

    CustomSecretsManagerShellScript templatizedShellScript =
        customSecretsManagerConfig.getCustomSecretsManagerShellScript();
    String scriptContent = resolveVariables(customSecretsManagerConfig.getAccountId(),
        templatizedShellScript.getScriptString(), encryptedData.getParameters());
    CustomSecretsManagerShellScript resolvedShellScript = CustomSecretsManagerShellScript.builder()
                                                              .scriptString(scriptContent)
                                                              .scriptType(templatizedShellScript.getScriptType())
                                                              .timeoutMillis(templatizedShellScript.getTimeoutMillis())
                                                              .variables(templatizedShellScript.getVariables())
                                                              .build();
    customSecretsManagerConfig.setCustomSecretsManagerShellScript(resolvedShellScript);

    if (!customSecretsManagerConfig.isExecuteOnDelegate()) {
      if (customSecretsManagerConfig.isConnectorTemplatized()) {
        customSecretsManagerConnectorHelper.setConnectorInConfig(
            customSecretsManagerConfig, encryptedData.getParameters());
      }
      managerDecryptionService.decrypt(customSecretsManagerConfig.getRemoteHostConnector(),
          secretManager.getEncryptionDetails(customSecretsManagerConfig.getRemoteHostConnector()));
    }
    EncryptedRecordData encryptedRecordData = SecretManager.buildRecordData(encryptedData);

    return EncryptedDataDetail.builder()
        .encryptedData(encryptedRecordData)
        .encryptionConfig(customSecretsManagerConfig)
        .build();
  }

  public void validateSecret(EncryptedData encryptedData, CustomSecretsManagerConfig customSecretsManagerConfig) {
    EncryptedDataDetail encryptedDataDetail = buildEncryptedDataDetail(encryptedData, customSecretsManagerConfig);
    managerDecryptionService.fetchSecretValue(customSecretsManagerConfig.getAccountId(), encryptedDataDetail);
  }

  private String resolveVariables(String accountId, String script, Set<EncryptedDataParams> parameters) {
    Map<String, Object> context = new HashMap<>();
    parameters.forEach(secretVariable -> context.put(secretVariable.getName(), secretVariable.getValue()));
    String scriptWithResolvedVariables = expressionEvaluator.substitute(script, context);
    context.put("secrets",
        SecretFunctor.builder()
            .managerDecryptionService(managerDecryptionService)
            .secretManager(secretManager)
            .accountId(accountId)
            .build());
    return expressionEvaluator.substitute(scriptWithResolvedVariables, context);
  }
}
