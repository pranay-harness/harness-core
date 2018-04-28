package software.wings.delegatetasks.validation;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import software.wings.beans.DelegateTask;
import software.wings.helpers.ext.external.comm.CollaborationProviderRequest;
import software.wings.helpers.ext.external.comm.handlers.EmailHandler;

import java.util.List;
import java.util.function.Consumer;

public class CollaborationProviderTaskValidation extends AbstractDelegateValidateTask {
  @Inject EmailHandler emailHandler;
  public CollaborationProviderTaskValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTask, consumer);
  }

  @Override
  public List<String> getCriteria() {
    Object[] parameters = getParameters();
    CollaborationProviderRequest request = (CollaborationProviderRequest) parameters[0];
    return request.getCriteria();
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    try {
      Object[] parameters = getParameters();
      CollaborationProviderRequest request = (CollaborationProviderRequest) parameters[0];
      String criteria = getCriteria().get(0);
      boolean validForDelegate = false;
      switch (request.getCommunicationType()) {
        case EMAIL:
          validForDelegate = emailHandler.validateDelegateConnection(request);
          break;
        default:
          validForDelegate = false;
      }
      return singletonList(DelegateConnectionResult.builder().criteria(criteria).validated(validForDelegate).build());
    } catch (Exception e) {
      return emptyList();
    }
  }
}
