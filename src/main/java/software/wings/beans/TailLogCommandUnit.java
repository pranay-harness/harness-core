package software.wings.beans;

import static software.wings.beans.CommandUnitType.TAIL_LOG;

import com.google.common.base.Strings;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;

/**
 * Created by anubhaw on 7/13/16.
 */
public class TailLogCommandUnit extends ExecCommandUnit {
  @Attributes(title = "File path") private String filePath;
  @Attributes(title = "Search text") private String searchString;
  @Attributes(title = "Timeout duration (default 30 seconds)", description = "Timeout in seconds")
  private int timeout = 30 * 1000;

  /**
   * Instantiates a new Tail log command unit.
   */
  public TailLogCommandUnit() {
    super();
    setCommandUnitType(TAIL_LOG);
  }

  @Override
  public void setup(CommandExecutionContext context) {
    filePath = context.getRuntimePath() + "/" + filePath;
    setCommandString(String.format("tail -f -n +1 %s | grep '%s'", filePath, searchString));
  }

  @Override
  public CommandUnitExecutionResult processCommandOutput(String line) {
    CommandUnitExecutionResult stop = CommandUnitExecutionResult.STOP;
    stop.setExecutionResult(!Strings.isNullOrEmpty(line) ? ExecutionResult.SUCCESS : ExecutionResult.FAILURE);
    return stop;
  }

  @SchemaIgnore
  @Override
  public int getCommandExecutionTimeout() {
    return timeout;
  }

  /**
   * Gets file path.
   *
   * @return the file path
   */
  public String getFilePath() {
    return filePath;
  }

  /**
   * Sets file path.
   *
   * @param filePath the file path
   */
  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  /**
   * Gets search string.
   *
   * @return the search string
   */
  public String getSearchString() {
    return searchString;
  }

  /**
   * Sets search string.
   *
   * @param searchString the search string
   */
  public void setSearchString(String searchString) {
    this.searchString = searchString;
  }

  /**
   * Gets timeout.
   *
   * @return the timeout
   */
  public int getTimeout() {
    return timeout;
  }

  /**
   * Sets timeout.
   *
   * @param timeout the timeout
   */
  public void setTimeout(int timeout) {
    this.timeout = timeout * 1000;
  }
}
