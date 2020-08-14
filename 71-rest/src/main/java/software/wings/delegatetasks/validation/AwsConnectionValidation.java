package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import io.harness.delegate.beans.DelegateTaskPackage;

import java.util.List;
import java.util.function.Consumer;

public class AwsConnectionValidation extends AbstractDelegateValidateTask {
  private static final String AWS_URL = "https://aws.amazon.com/";
  public AwsConnectionValidation(
      String delegateId, DelegateTaskPackage delegateTaskPackage, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTaskPackage, consumer);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(AWS_URL);
  }
}