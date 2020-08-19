package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectionTaskParams;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import software.wings.beans.AppDynamicsConfig;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class AppDynamicsNGValidation extends AbstractSecretManagerValidation {
  public AppDynamicsNGValidation(String delegateId, DelegateTaskPackage delegateTaskPackage,
      Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTaskPackage, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    Object[] parameters = getParameters();
    if (parameters[0] instanceof AppDynamicsConnectionTaskParams) {
      AppDynamicsConnectionTaskParams taskParams = (AppDynamicsConnectionTaskParams) parameters[0];
      return singletonList(taskParams.getAppDynamicsConnectorDTO().getControllerUrl());
    }
    return singletonList(Arrays.stream(parameters)
                             .filter(o -> o instanceof AppDynamicsConnectorDTO || o instanceof AppDynamicsConfig)
                             .map(obj -> ((AppDynamicsConnectorDTO) obj).getControllerUrl())
                             .findFirst()
                             .orElse(null));
  }
}