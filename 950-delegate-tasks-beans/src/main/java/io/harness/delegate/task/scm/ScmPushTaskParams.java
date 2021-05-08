package io.harness.delegate.task.scm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitFileDetails;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.git.model.ChangeType;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.DX)
public class ScmPushTaskParams implements TaskParameters, ExecutionCapabilityDemander {
  ScmConnector scmConnector;
  GitFileDetails gitFileDetails;
  ChangeType changeType;
  List<EncryptedDataDetail> encryptedDataDetails;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return Collections.emptyList();
  }
}
