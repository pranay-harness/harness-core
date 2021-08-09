package software.wings.beans.servicenow;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.ServiceNowConfig;
import software.wings.delegatetasks.servicenow.ServiceNowAction;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowFieldType;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowTicketType;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString(exclude = {"encryptionDetails"})
public class ServiceNowTaskParameters implements TaskParameters, ExecutionCapabilityDemander {
  private String accountId;
  private ServiceNowConfig serviceNowConfig;
  private ServiceNowAction action;
  private String issueNumber;
  private String issueId;
  List<EncryptedDataDetail> encryptionDetails;
  private ServiceNowTicketType ticketType;
  private Map<ServiceNowFields, String> fields;
  private Map<String, String> additionalFields;
  private boolean updateMultiple;
  private ServiceNowFieldType typeFilter;
  // import set fields
  private String importSetTableName;
  private String jsonBody;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    // As it extends TaskParameters, no need to pass encryptionDetails.
    // It will be resolved to valut capability in DelegateSErviceImp
    return serviceNowConfig.fetchRequiredExecutionCapabilities(maskingEvaluator);
  }
}
