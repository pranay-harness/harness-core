package software.wings.beans.servicenow;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import software.wings.beans.ServiceNowConfig;
import software.wings.delegatetasks.servicenow.ServiceNowAction;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowTicketType;

import java.util.List;

@Data
@Builder
@ToString(exclude = {"encryptionDetails"})
public class ServiceNowTaskParameters implements TaskParameters, ExecutionCapabilityDemander {
  private String accountId;
  private ServiceNowConfig serviceNowConfig;
  private ServiceNowAction action;
  private String issueNumber;
  List<EncryptedDataDetail> encryptionDetails;
  private ServiceNowTicketType ticketType;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    // As it extends TaskParameters, no need to pass encryptionDetails.
    // It will be resolved to valut capability in DelegateSErviceImp
    return serviceNowConfig.fetchRequiredExecutionCapabilities();
  }
}
