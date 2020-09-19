package io.harness.cdng.connector.tasks;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskParams;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskResponse;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.k8s.K8sYamlToDelegateDTOMapper;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.security.encryption.SecretDecryptionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
public class KubernetesTestConnectionDelegateTask extends AbstractDelegateRunnableTask {
  @Inject private SecretDecryptionService secretDecryptionService;
  @Inject private K8sYamlToDelegateDTOMapper k8sYamlToDelegateDTOMapper;
  @Inject private KubernetesContainerService kubernetesContainerService;
  private static final String EMPTY_STR = "";

  public KubernetesTestConnectionDelegateTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public KubernetesConnectionTaskResponse run(TaskParameters parameters) {
    KubernetesConnectionTaskParams kubernetesConnectionTaskParams = (KubernetesConnectionTaskParams) parameters;
    KubernetesClusterConfigDTO kubernetesClusterConfig = kubernetesConnectionTaskParams.getKubernetesClusterConfig();
    if (kubernetesClusterConfig.getKubernetesCredentialType() == KubernetesCredentialType.MANUAL_CREDENTIALS) {
      KubernetesAuthCredentialDTO kubernetesCredentialAuth =
          getKubernetesCredentialsAuth((KubernetesClusterDetailsDTO) kubernetesClusterConfig.getConfig());
      secretDecryptionService.decrypt(kubernetesCredentialAuth, kubernetesConnectionTaskParams.getEncryptionDetails());
    }
    Exception execptionInProcessing = null;
    boolean validCredentials = false;
    KubernetesConfig kubernetesConfig =
        k8sYamlToDelegateDTOMapper.createKubernetesConfigFromClusterConfig(kubernetesClusterConfig);
    try {
      kubernetesContainerService.validate(kubernetesConfig);
      validCredentials = true;
    } catch (Exception ex) {
      logger.info("Exception while validating kubernetes credentials", ex);
      execptionInProcessing = ex;
    }
    return KubernetesConnectionTaskResponse.builder()
        .connectionSuccessFul(validCredentials)
        .errorMessage(execptionInProcessing != null ? execptionInProcessing.getMessage() : EMPTY_STR)
        .build();
  }

  private KubernetesAuthCredentialDTO getKubernetesCredentialsAuth(
      KubernetesClusterDetailsDTO kubernetesClusterConfigDTO) {
    return kubernetesClusterConfigDTO.getAuth().getCredentials();
  }

  @Override
  public KubernetesConnectionTaskResponse run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }
}
