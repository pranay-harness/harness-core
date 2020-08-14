package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskPackage;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.container.KubernetesSwapServiceSelectorsParams;
import software.wings.service.impl.ContainerServiceParams;

import java.util.List;
import java.util.function.Consumer;

public class KubernetesSwapServiceSelectorsValidation extends AbstractDelegateValidateTask {
  @Inject @Transient private transient ContainerValidationHelper containerValidationHelper;

  public KubernetesSwapServiceSelectorsValidation(String delegateId, DelegateTaskPackage delegateTaskPackage,
      Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTaskPackage, postExecute);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    ContainerServiceParams containerServiceParams =
        ((KubernetesSwapServiceSelectorsParams) getParameters()[0]).getContainerServiceParams();
    return singletonList(
        DelegateConnectionResult.builder()
            .criteria(getCriteria(containerServiceParams))
            .validated(containerValidationHelper.validateContainerServiceParams(containerServiceParams))
            .build());
  }

  @Override
  public List<String> getCriteria() {
    KubernetesSwapServiceSelectorsParams kubernetesSwapServiceSelectorsParams =
        (KubernetesSwapServiceSelectorsParams) getParameters()[0];
    return singletonList(getCriteria(kubernetesSwapServiceSelectorsParams.getContainerServiceParams()));
  }

  private String getCriteria(ContainerServiceParams containerServiceParams) {
    return containerValidationHelper.getCriteria(containerServiceParams);
  }
}
