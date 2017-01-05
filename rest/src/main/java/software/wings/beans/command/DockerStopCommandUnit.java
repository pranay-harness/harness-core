package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.stencils.DefaultValue;

import java.util.List;

/**
 * Created by anubhaw on 1/4/17.
 */
@JsonTypeName("DOCKER_STOP")
public class DockerStopCommandUnit extends ExecCommandUnit {
  /**
   * Instantiates a new Docker run command unit.
   */
  public DockerStopCommandUnit() {
    super();
    setCommandUnitType(CommandUnitType.DOCKER_STOP);
  }

  @Attributes(title = "Command")
  @DefaultValue("docker ps -a -q --filter ancestor=$IMAGE | xargs docker stop")
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
  public List<TailFilePatternEntry> getTailPatterns() {
    return super.getTailPatterns();
  }
}
