package software.wings.beans;

import static java.lang.String.format;

/**
 * Created by anubhaw on 7/12/16.
 */
public class SetupEnvCommandUnit extends AbstractExecCommandUnit {
  /**
   * Instantiates a new Setup env command unit.
   */
  public SetupEnvCommandUnit() {
    super(CommandUnitType.SETUP_ENV);
  }

  @Override
  public void setup(CommandExecutionContext context) {
    setCommand(format(
        "set -m; mkdir -p %s %s %s", context.getRuntimePath(), context.getBackupPath(), context.getStagingPath()));
  }
}
