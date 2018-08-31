package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.PcfConfig;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;
import java.util.function.Consumer;

public class PCFCommandValidation extends AbstractDelegateValidateTask {
  private static final Logger logger = LoggerFactory.getLogger(PCFCommandValidation.class);
  public static final String CONNECTION_TIMED_OUT = "connection timed out";

  @Inject private transient PcfDeploymentManager pcfDeploymentManager;
  @Inject private transient EncryptionService encryptionService;

  public PCFCommandValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTask, consumer);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<DelegateConnectionResult> validate() {
    boolean validated = false;
    PcfCommandRequest commandRequest = (PcfCommandRequest) getParameters()[0];
    PcfConfig pcfConfig = commandRequest.getPcfConfig();

    List<EncryptedDataDetail> encryptionDetails = (List<EncryptedDataDetail>) getParameters()[1];
    logger.info("Running validation for task {} ", delegateTaskId);

    try {
      if (encryptionDetails != null) {
        encryptionService.decrypt(pcfConfig, encryptionDetails);
      }

      String validationErrorMsg = pcfDeploymentManager.checkConnectivity(pcfConfig);
      validated = !validationErrorMsg.toLowerCase().contains(CONNECTION_TIMED_OUT);
    } catch (Exception e) {
      String errorMsg = new StringBuilder(64)
                            .append("Failed to Decrypt pcfConfig, ")
                            .append("RepoUrl: ")
                            .append(pcfConfig.getEndpointUrl())
                            .toString();
      logger.error(errorMsg);
    }

    if (!validated) {
      logger.warn("This delegate failed to verify Pivotal connectivity");
    }

    return singletonList(
        DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(validated).build());
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(getCriteria((PcfCommandRequest) getParameters()[0]));
  }

  private String getCriteria(PcfCommandRequest pcfCommandRequest) {
    PcfConfig pcfConfig = pcfCommandRequest.getPcfConfig();
    return new StringBuilder()
        .append("Pcf:")
        .append(pcfConfig.getEndpointUrl())
        .append("/")
        .append(pcfConfig.getUsername())
        .toString();
  }
}