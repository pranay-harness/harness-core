package software.wings.helpers.ext.external.comm;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.ProcessExecutorCapabilityGenerator;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.Arrays;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class EmailRequest extends CollaborationProviderRequest {
  private EmailData emailData;
  private SmtpConfig smtpConfig;
  private List<EncryptedDataDetail> encryptionDetails;

  @Builder
  public EmailRequest(CommunicationType communicationType, EmailData emailData, SmtpConfig smtpConfig,
      List<EncryptedDataDetail> encryptionDetails) {
    super(communicationType);
    this.emailData = emailData;
    this.smtpConfig = smtpConfig;
    this.encryptionDetails = encryptionDetails;
  }

  public EmailRequest(CommunicationType communicationType) {
    super(communicationType);
  }

  @Override
  public CommunicationType getCommunicationType() {
    return CommunicationType.EMAIL;
  }

  @Override
  public List<String> getCriteria() {
    return Arrays.asList(smtpConfig.getHost() + ":" + smtpConfig.getPort());
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    List<String> processExecutorArgs =
        Arrays.asList("nc", "-z", "-G5", smtpConfig.getHost(), Integer.toString(smtpConfig.getPort()));
    return Arrays.asList(ProcessExecutorCapabilityGenerator.buildProcessExecutorCapability(
        getCommunicationType().name(), processExecutorArgs));
  }
}
