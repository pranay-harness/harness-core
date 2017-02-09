package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.stencils.DefaultValue;

import java.util.List;

/**
 * Created by anubhaw on 1/4/17.
 */
@JsonTypeName("DOCKER_START")
public class DockerStartCommandUnit extends ExecCommandUnit {
  /**
   * Instantiates a new Docker run command unit.
   */
  public DockerStartCommandUnit() {
    super();
    super.setArtifactNeeded(true);
    setCommandUnitType(CommandUnitType.DOCKER_START);
  }

  @Attributes(title = "Command")
  @DefaultValue(
      "docker login --username=\"$DOCKER_USER_ID\" --password=\"$DOCKER_USER_PASSWORD\"\ndocker run -d -it \"$DOCKER_IMAGE\"\ndocker logout")
  @Override
  public String
  getCommandString() {
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
