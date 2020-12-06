package software.wings.beans.trigger;

public interface TriggerCommand {
  TriggerCommandType getTriggerCommandType();

  enum TriggerCommandType {
    DEPLOYMENT_NEEDED_CHECK,
  }
}
