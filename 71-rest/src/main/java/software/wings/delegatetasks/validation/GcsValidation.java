package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import software.wings.beans.DelegateTaskPackage;

import java.util.List;
import java.util.function.Consumer;

public class GcsValidation extends AbstractDelegateValidateTask {
  private static final String GCS_URL = "https://storage.cloud.google.com/";
  public GcsValidation(String delegateId, DelegateTaskPackage delegateTaskPackage,
      Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTaskPackage, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(GCS_URL);
  }
}
