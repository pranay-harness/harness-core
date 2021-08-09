package io.harness.delegatetasks;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.helpers.ext.vault.SecretEngineSummary;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@OwnedBy(PL)
@Getter
@Builder
public class NGVaultFetchEngineTaskResponse implements DelegateTaskNotifyResponseData {
  @Setter private DelegateMetaInfo delegateMetaInfo;
  private final List<SecretEngineSummary> secretEngineSummaryList;
}
