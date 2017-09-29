package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.stencils.DefaultValue;

import java.util.List;

/**
 * Created by anubhaw on 7/12/16.
 */
@JsonTypeName("SETUP_ENV")
public class SetupEnvCommandUnit extends ExecCommandUnit {
  public static final String setupEnvCommandString = "# Execute as root and pass environment variables\n"
      + "# su -p -\n\n"
      + "# Execute as root via user credentials (with root privileges)\n"
      + "# sudo -E su -p -\n\n"
      + "mkdir -p \"$WINGS_RUNTIME_PATH\"\n"
      + "mkdir -p \"$WINGS_BACKUP_PATH\"\n"
      + "mkdir -p \"$WINGS_STAGING_PATH\"";

  /**
   * Instantiates a new Setup env command unit.
   */
  public SetupEnvCommandUnit() {
    super();
    setCommandUnitType(CommandUnitType.SETUP_ENV);
  }

  @SchemaIgnore
  @Override
  public String getPreparedCommand() {
    return super.getPreparedCommand();
  }

  @Attributes(title = "Command")
  @DefaultValue(setupEnvCommandString)
  @Override
  public String getCommandString() {
    return super.getCommandString();
  }

  @Attributes(title = "Working Directory")
  @Override
  public String getCommandPath() {
    return super.getCommandPath();
  }

  @SchemaIgnore
  @Override
  public List<TailFilePatternEntry> getTailPatterns() {
    return super.getTailPatterns();
  }
}
