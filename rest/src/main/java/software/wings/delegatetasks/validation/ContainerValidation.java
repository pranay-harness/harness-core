package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.DelegateTask;
import software.wings.service.impl.ContainerServiceParams;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/29/17
 */
public class ContainerValidation extends AbstractDelegateValidateTask {
  @Inject @Transient private transient ContainerValidationHelper containerValidationHelper;

  public ContainerValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    ContainerServiceParams containerServiceParams = (ContainerServiceParams) getParameters()[2];
    return containerValidationHelper.validateContainerServiceParams(containerServiceParams);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(containerValidationHelper.getCriteria((ContainerServiceParams) getParameters()[2]));
  }
}
