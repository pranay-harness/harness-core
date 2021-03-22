package io.harness.cvng;

import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OnboardingTaskParameters implements TaskParameters {
  @NotNull private String accountId;
  @NotNull private DataCollectionRequest dataCollectionRequest;

  private List<EncryptedDataDetail> encryptedDataDetails;
}
