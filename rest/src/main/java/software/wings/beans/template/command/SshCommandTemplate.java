package software.wings.beans.template.command;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.template.BaseTemplate;

import java.util.List;

@JsonTypeName("SSH")
@Value
@Builder
public class SshCommandTemplate implements BaseTemplate {
  private CommandType commandType;
  @JsonInclude(NON_NULL) private transient List<AbstractCommandUnit.Yaml> commands;
  @Wither private List<CommandUnit> commandUnits;
}
