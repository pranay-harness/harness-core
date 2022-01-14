package io.harness.cvng.client;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CV)
public class NonPrivilegedNextGenServiceImpl extends NextGenServiceImpl {
  @Inject
  public NonPrivilegedNextGenServiceImpl(
      @Named("NON_PRIVILEGED") NextGenClient nextGenClient, RequestExecutor requestExecutor) {
    super(nextGenClient, requestExecutor);
  }
}
