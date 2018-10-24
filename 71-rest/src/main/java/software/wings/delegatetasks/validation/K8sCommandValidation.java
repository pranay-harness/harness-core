package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.DelegateTask;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sCommandRequest;

import java.util.List;
import java.util.function.Consumer;

public class K8sCommandValidation extends AbstractDelegateValidateTask {
  @Inject @Transient private transient K8sValidationHelper k8sValidationHelper;

  public K8sCommandValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    K8sClusterConfig k8sClusterConfig = ((K8sCommandRequest) getParameters()[0]).getK8sClusterConfig();
    return singletonList(DelegateConnectionResult.builder()
                             .criteria(getCriteria(k8sClusterConfig))
                             .validated(k8sValidationHelper.validateContainerServiceParams(k8sClusterConfig))
                             .build());
  }

  @Override
  public List<String> getCriteria() {
    K8sCommandRequest request = (K8sCommandRequest) getParameters()[0];
    return singletonList(getCriteria(request.getK8sClusterConfig()));
  }

  private String getCriteria(K8sClusterConfig k8sClusterConfig) {
    return k8sValidationHelper.getCriteria(k8sClusterConfig);
  }
}
