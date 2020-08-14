package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.task.http.HttpTaskParameters;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class HttpValidation extends AbstractDelegateValidateTask {
  public HttpValidation(String delegateId, DelegateTaskPackage delegateTaskPackage,
      Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTaskPackage, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    HttpTaskParameters parameters = (HttpTaskParameters) getParameters()[0];
    return singletonList(parameters.getUrl());
  }
}
