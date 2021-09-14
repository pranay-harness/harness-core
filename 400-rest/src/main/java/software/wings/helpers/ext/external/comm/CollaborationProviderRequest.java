/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.helpers.ext.external.comm;

import io.harness.delegate.beans.executioncapability.AlwaysFalseValidationCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.expression.ExpressionEvaluator;

import java.util.Collections;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

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
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return Collections.singletonList(AlwaysFalseValidationCapability.builder().build());
  }
}
