package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import software.wings.beans.DelegateTask;

import java.util.List;
import java.util.function.Consumer;

public class GcsValidation extends AbstractDelegateValidateTask {
  private static final String GCS_URL = "https://storage.cloud.google.com/";
  public GcsValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(GCS_URL);
  }
}
