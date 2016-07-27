package software.wings.beans.command;

/**
 * Created by anubhaw on 7/12/16.
 */
public class SetupEnvCommandUnit extends ExecCommandUnit {
  /**
   * Instantiates a new Setup env command unit.
   */
  public SetupEnvCommandUnit() {
    super();
    setCommandUnitType(CommandUnitType.SETUP_ENV);
    setCommandString("mkdir -p $RUNTIME_PATH\n"
        + "mkdir -p $BACKUP_PATH\n"
        + "mkdir -p $STAGING_PATH");
  }
}
