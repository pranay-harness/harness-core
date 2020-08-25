package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectionTaskParams;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class SplunkNGValidation extends AbstractSecretManagerValidation {
  SplunkNGValidation(String delegateId, DelegateTaskPackage delegateTaskPackage,
      Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTaskPackage, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    Object[] parameters = getParameters();
    if (parameters[0] instanceof SplunkConnectionTaskParams) {
      SplunkConnectionTaskParams taskParams = (SplunkConnectionTaskParams) parameters[0];
      return singletonList(taskParams.getSplunkConnectorDTO().getSplunkUrl());
    }
    return singletonList(Arrays.stream(parameters)
                             .filter(o -> o instanceof SplunkConnectorDTO)
                             .map(obj -> ((SplunkConnectorDTO) obj).getSplunkUrl())
                             .findFirst()
                             .orElse(null));
  }
}
