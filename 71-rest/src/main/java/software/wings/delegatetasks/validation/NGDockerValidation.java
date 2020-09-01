package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.connector.docker.DockerTestConnectionTaskParams;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class NGDockerValidation extends AbstractDelegateValidateTask {
  public NGDockerValidation(String delegateId, DelegateTaskPackage delegateTaskPackage,
      Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTaskPackage, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(Arrays.stream(getParameters())
                             .filter(o -> o instanceof DockerTestConnectionTaskParams)
                             .map(config -> (((DockerTestConnectionTaskParams) config).getDockerConnector()).getUrl())
                             .findFirst()
                             .orElse(null));
  }
}