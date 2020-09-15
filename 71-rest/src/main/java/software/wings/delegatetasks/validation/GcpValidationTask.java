package software.wings.delegatetasks.validation;

import io.harness.delegate.beans.DelegateTaskPackage;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class GcpValidationTask extends AbstractDelegateValidateTask {
  private static final String GCP_URL = "https://cloud.google.com/";

  public GcpValidationTask(
      String delegateId, DelegateTaskPackage delegateTaskPackage, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTaskPackage, consumer);
  }

  @Override
  public List<String> getCriteria() {
    return Collections.singletonList(GCP_URL);
  }
}
