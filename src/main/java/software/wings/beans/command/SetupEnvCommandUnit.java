package software.wings.beans.command;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.stencils.DefaultValue;

import java.util.List;

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
  }

  @SchemaIgnore
  @Override
  public String getPreparedCommand() {
    return super.getPreparedCommand();
  }

  @Attributes(title = "Command")
  @DefaultValue("mkdir -p \"$WINGS_RUNTIME_PATH\"\nmkdir -p \"$WINGS_BACKUP_PATH\"\nmkdir -p \"$WINGS_STAGING_PATH\"")
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
  public boolean isTailFiles() {
    return super.isTailFiles();
  }

  @SchemaIgnore
  @Override
  public List<TailFilePatternEntry> getTailPatterns() {
    return super.getTailPatterns();
  }
}
