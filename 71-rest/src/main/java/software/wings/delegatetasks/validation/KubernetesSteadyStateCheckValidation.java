package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.DelegateTask;
import software.wings.beans.container.KubernetesSteadyStateCheckParams;
import software.wings.service.impl.ContainerServiceParams;

import java.util.List;
import java.util.function.Consumer;

public class KubernetesSteadyStateCheckValidation extends AbstractDelegateValidateTask {
  @Inject @Transient private transient ContainerValidationHelper containerValidationHelper;

  public KubernetesSteadyStateCheckValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    ContainerServiceParams containerServiceParams =
        ((KubernetesSteadyStateCheckParams) getParameters()[0]).getContainerServiceParams();
    return singletonList(
        DelegateConnectionResult.builder()
            .criteria(getCriteria(containerServiceParams))
            .validated(containerValidationHelper.validateContainerServiceParams(containerServiceParams))
            .build());
  }

  @Override
  public List<String> getCriteria() {
    KubernetesSteadyStateCheckParams kubernetesSteadyStateCheckParams =
        (KubernetesSteadyStateCheckParams) getParameters()[0];
    return singletonList(getCriteria(kubernetesSteadyStateCheckParams.getContainerServiceParams()));
  }

  private String getCriteria(ContainerServiceParams containerServiceParams) {
    return containerValidationHelper.getCriteria(containerServiceParams);
  }
}
