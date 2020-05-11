package software.wings.beans.template.command;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.ReferencedTemplate;

import java.util.List;

@OwnedBy(CDC)
@JsonTypeName("SSH")
@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
public class SshCommandTemplate implements BaseTemplate {
  private CommandType commandType;
  @JsonInclude(NON_NULL) private transient List<AbstractCommandUnit.Yaml> commands;
  @Wither private List<CommandUnit> commandUnits;
  @Wither @JsonIgnore private List<ReferencedTemplate> referencedTemplateList;
}
