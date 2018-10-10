package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AzureConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.azure.AcrService;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class AcrValidation extends AbstractDelegateValidateTask {
  private static final Logger logger = LoggerFactory.getLogger(AcrValidation.class);

  @Inject private transient AcrService acrService;

  public AcrValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(Arrays.stream(getParameters())
                             .filter(o -> o instanceof ArtifactStreamAttributes)
                             .map(config -> getAcrCriteria((ArtifactStreamAttributes) config))
                             .findFirst()
                             .orElse(null));
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<DelegateConnectionResult> validate() {
    boolean validated = false;

    List<EncryptedDataDetail> encryptionDetails =
        (List<EncryptedDataDetail>) Arrays.stream(getParameters())
            .filter(o -> o instanceof List && ((List) o).size() > 0 && ((List) o).get(0) instanceof EncryptedDataDetail)
            .findFirst()
            .get();

    AzureConfig azureConfig =
        (AzureConfig) Arrays.stream(getParameters()).filter(o -> o instanceof AzureConfig).findFirst().get();

    ArtifactStreamAttributes artifactStreamAttributes = (ArtifactStreamAttributes) Arrays.stream(getParameters())
                                                            .filter(o -> o instanceof ArtifactStreamAttributes)
                                                            .findFirst()
                                                            .get();

    logger.info("Running validation for task {} ", delegateTaskId);

    try {
      validated = acrService.validateCredentials(azureConfig, encryptionDetails, artifactStreamAttributes);
    } catch (Exception e) {
      logger.warn("ACR Validation failed", e);
    }

    return singletonList(
        DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(validated).build());
  }

  private String getAcrCriteria(ArtifactStreamAttributes artifactStreamAttributes) {
    return String.format(
        "ACR_%s_%s", artifactStreamAttributes.getRegistryName(), artifactStreamAttributes.getRepositoryName());
  }
}