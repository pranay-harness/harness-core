package software.wings.sm.states;

import software.wings.sm.StateType;

public class AwsLambdaRollback extends AwsLambdaState {
  public AwsLambdaRollback(String name) {
    super(name, StateType.AWS_LAMBDA_ROLLBACK.name());
  }
}
