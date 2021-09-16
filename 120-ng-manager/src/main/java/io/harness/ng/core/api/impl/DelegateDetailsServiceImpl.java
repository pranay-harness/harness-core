package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.api.DelegateDetailsService;

import com.google.inject.Inject;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;

@OwnedBy(DEL)
@RequiredArgsConstructor(onConstructor = @__({ @Inject }))
public class DelegateDetailsServiceImpl implements DelegateDetailsService {
  @Override
  public long getDelegateGroupCount(
      final String accountId, @Nullable final String orgId, @Nullable final String projectId) {
    return 0;
  }
}
