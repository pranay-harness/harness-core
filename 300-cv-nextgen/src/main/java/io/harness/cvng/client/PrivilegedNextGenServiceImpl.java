package io.harness.cvng.client;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CV)
public class PrivilegedNextGenServiceImpl extends NextGenServiceImpl {
  @Inject
  public PrivilegedNextGenServiceImpl(
      @Named("PRIVILEGED") NextGenClient nextGenClient, RequestExecutor requestExecutor) {
    super(nextGenClient, requestExecutor);
  }
}
