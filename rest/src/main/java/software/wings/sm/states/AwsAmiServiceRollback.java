package software.wings.sm.states;

import software.wings.sm.StateType;

/**
 * Created by anubhaw on 12/19/17.
 */
public class AwsAmiServiceRollback extends AwsAmiServiceDeployState {
  /**
   * Instantiates a new state.
   *
   * @param name      the name
   */
  public AwsAmiServiceRollback(String name) {
    super(name, StateType.AWS_AMI_SERVICE_ROLLBACK.name());
  }
}
