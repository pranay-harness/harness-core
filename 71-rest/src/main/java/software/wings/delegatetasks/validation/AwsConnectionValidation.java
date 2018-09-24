package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import software.wings.beans.DelegateTask;

import java.util.List;
import java.util.function.Consumer;

public class AwsConnectionValidation extends AbstractDelegateValidateTask {
  private static final String AWS_URL = "https://aws.amazon.com/";
  public AwsConnectionValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTask, consumer);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(AWS_URL);
  }
}