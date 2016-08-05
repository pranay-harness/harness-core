package software.wings.beans.command;

import static com.google.common.collect.ImmutableMap.of;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import freemarker.template.TemplateException;
import software.wings.stencils.DataProvider;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by peeyushaggarwal on 8/3/16.
 */
public class PortCheckCommandUnit extends ExecCommandUnit {
  @Attributes(title = "Ensure port", description = "ex. 8080") private int port;

  @Attributes(title = "Is Open?") private boolean open;

  public PortCheckCommandUnit() {
    super();
    setCommandUnitType(CommandUnitType.PORT_CHECK);
  }

  @Override
  public List<String> prepare(String activityId, String executionStagingDir, String launcherScriptFileName,
      String prefix) throws IOException, TemplateException {
    setCommandString("set -x\nnc -v -z -w 5 localhost " + port);
    if (!open) {
      setCommandString(getCommandString() + "\nrc=$?"
          + "\nif [ \"$rc\" -eq 0 ]\nthen\nexit 1\nfi");
    }
    return super.prepare(activityId, executionStagingDir, launcherScriptFileName, prefix);
  }

  /**
   * Getter for property 'port'.
   *
   * @return Value for property 'port'.
   */
  public int getPort() {
    return port;
  }

  /**
   * Setter for property 'port'.
   *
   * @param port Value to set for property 'port'.
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Getter for property 'open'.
   *
   * @return Value for property 'open'.
   */
  public boolean isOpen() {
    return open;
  }

  /**
   * Setter for property 'open'.
   *
   * @param open Value to set for property 'open'.
   */
  public void setOpen(boolean open) {
    this.open = open;
  }

  @SchemaIgnore
  @Override
  public String getCommandString() {
    return super.getCommandString();
  }

  @SchemaIgnore
  @Override
  public String getCommandPath() {
    return super.getCommandPath();
  }

  @SchemaIgnore
  @Override
  public String getPreparedCommand() {
    return super.getPreparedCommand();
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

  public static class PortCheckDataProvider implements DataProvider {
    @Override
    public Map<String, String> getData(String appId, String... params) {
      return of("true", "Free", "false", "Used");
    }
  }
}
