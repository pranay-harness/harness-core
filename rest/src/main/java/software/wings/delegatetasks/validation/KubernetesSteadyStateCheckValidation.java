package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.DelegateTask;
import software.wings.beans.container.KubernetesSteadyStateCheckParams;

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
    KubernetesSteadyStateCheckParams kubernetesSteadyStateCheckParams =
        (KubernetesSteadyStateCheckParams) getParameters()[0];
    return containerValidationHelper.validateContainerServiceParams(
        kubernetesSteadyStateCheckParams.getContainerServiceParams());
  }

  @Override
  public List<String> getCriteria() {
    KubernetesSteadyStateCheckParams kubernetesSteadyStateCheckParams =
        (KubernetesSteadyStateCheckParams) getParameters()[0];
    return singletonList(
        containerValidationHelper.getCriteria(kubernetesSteadyStateCheckParams.getContainerServiceParams()));
  }
}
