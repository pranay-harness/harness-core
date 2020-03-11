package software.wings.helpers.ext.external.comm;

import io.harness.delegate.beans.executioncapability.AlwaysFalseValidationCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
public abstract class CollaborationProviderRequest implements ExecutionCapabilityDemander {
  @NotEmpty private CommunicationType communicationType;

  public CollaborationProviderRequest(CommunicationType communicationType) {
    this.communicationType = communicationType;
  }

  public abstract CommunicationType getCommunicationType();

  public abstract List<String> getCriteria();

  public enum CommunicationType { EMAIL }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Collections.singletonList(AlwaysFalseValidationCapability.builder().build());
  }
}
