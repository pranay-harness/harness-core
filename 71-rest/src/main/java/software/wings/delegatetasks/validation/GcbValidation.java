package software.wings.delegatetasks.validation;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import software.wings.helpers.ext.gcb.GcbRestClient;
import software.wings.helpers.ext.gcs.GcsRestClient;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
@OwnedBy(CDC)
public class GcbValidation extends AbstractDelegateValidateTask {
  public GcbValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return Arrays.asList(GcbRestClient.baseUrl, GcsRestClient.baseUrl);
  }
}
