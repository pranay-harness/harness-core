package software.wings.delegatetasks.validation;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.network.Http.connectableHttpUrl;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.DelegateTaskPackage;
import software.wings.helpers.ext.gcb.GcbServiceImpl;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@OwnedBy(CDC)
public class GcbValidation extends AbstractDelegateValidateTask {
  public GcbValidation(String delegateId, DelegateTaskPackage delegateTaskPackage,
      Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTaskPackage, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return Arrays.asList(GcbServiceImpl.GCB_BASE_URL, GcbServiceImpl.GCS_BASE_URL);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    return getCriteria()
        .stream()
        .map(criteria
            -> DelegateConnectionResult.builder().criteria(criteria).validated(connectableHttpUrl(criteria)).build())
        .collect(Collectors.toList());
  }
}
