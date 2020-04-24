package software.wings.delegatetasks.validation;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.Collections.singletonList;
import static software.wings.common.VerificationConstants.STACKDRIVER_URL;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.monitoring.v3.MonitoringScopes;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionConfig;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.GcpConfig;
import software.wings.service.impl.stackdriver.StackDriverDataCollectionInfo;
import software.wings.service.impl.stackdriver.StackDriverLogDataCollectionInfo;
import software.wings.service.impl.stackdriver.StackdriverGcpConfigTaskParams;
import software.wings.service.impl.stackdriver.StackdriverLogGcpConfigTaskParams;
import software.wings.service.intfc.security.EncryptionService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by Pranjal on 11/27/2018
 */
@Slf4j
public class StackDriverValidation extends AbstractSecretManagerValidation {
  @Inject private transient EncryptionService encryptionService;

  public StackDriverValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    if (!isEmpty(getParameters())) {
      GcpConfig gcpConfig;
      List<EncryptedDataDetail> encryptionDetails;
      if (getParameters()[0] instanceof StackDriverDataCollectionInfo) {
        gcpConfig = ((StackDriverDataCollectionInfo) getParameters()[0]).getGcpConfig();
        encryptionDetails = ((StackDriverDataCollectionInfo) getParameters()[0]).getEncryptedDataDetails();
      } else if (getParameters()[0] instanceof StackDriverLogDataCollectionInfo) {
        gcpConfig = ((StackDriverLogDataCollectionInfo) getParameters()[0]).getGcpConfig();
        encryptionDetails = ((StackDriverLogDataCollectionInfo) getParameters()[0]).getEncryptedDataDetails();
      } else if (getParameters()[2] instanceof StackdriverGcpConfigTaskParams) {
        gcpConfig = ((StackdriverGcpConfigTaskParams) getParameters()[2]).getGcpConfig();
        encryptionDetails = ((StackdriverGcpConfigTaskParams) getParameters()[2]).getEncryptedDataDetails();
      } else if (getParameters()[2] instanceof StackdriverLogGcpConfigTaskParams) {
        gcpConfig = ((StackdriverLogGcpConfigTaskParams) getParameters()[2]).getGcpConfig();
        encryptionDetails = ((StackdriverLogGcpConfigTaskParams) getParameters()[2]).getEncryptedDataDetails();
      } else {
        gcpConfig = (GcpConfig) getParameters()[2];
        encryptionDetails = (List<EncryptedDataDetail>) getParameters()[3];
      }
      try {
        encryptionService.decrypt(gcpConfig, encryptionDetails);
      } catch (Exception e) {
        logger.info("Failed to decrypt " + gcpConfig, e);
        return singletonList(
            DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(false).build());
      }

      try {
        GoogleCredential
            .fromStream(new ByteArrayInputStream(
                Charset.forName("UTF-8").encode(CharBuffer.wrap(gcpConfig.getServiceAccountKeyFileContent())).array()))
            .createScoped(MonitoringScopes.all());
      } catch (IOException e) {
        logger.info("Failed to connect " + gcpConfig, e);
        return singletonList(
            DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(false).build());
      }
      boolean validated = true;

      return singletonList(
          DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(validated).build());
    }
    return singletonList(DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(false).build());
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(STACKDRIVER_URL);
  }

  @Override
  protected EncryptionConfig getEncryptionConfig() {
    for (Object parmeter : getParameters()) {
      if (parmeter instanceof StackDriverDataCollectionInfo) {
        return ((StackDriverDataCollectionInfo) parmeter).getEncryptedDataDetails().get(0).getEncryptionConfig();
      }
    }
    return super.getEncryptionConfig();
  }
}
