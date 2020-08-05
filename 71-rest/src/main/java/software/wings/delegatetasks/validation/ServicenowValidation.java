package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.ServiceNowConfig;
import software.wings.beans.servicenow.ServiceNowTaskParameters;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ServicenowValidation extends AbstractDelegateValidateTask {
  public ServicenowValidation(
      String delegateId, DelegateTaskPackage delegateTaskPackage, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTaskPackage, consumer);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(
        Arrays.stream(getParameters())
            .filter(o -> o instanceof ServiceNowTaskParameters || o instanceof ServiceNowConfig)
            .map(obj
                -> (obj instanceof ServiceNowConfig ? (ServiceNowConfig) obj
                                                    : ((ServiceNowTaskParameters) obj).getServiceNowConfig())
                       .getBaseUrl())
            .findFirst()
            .orElse(null));
  }
}
