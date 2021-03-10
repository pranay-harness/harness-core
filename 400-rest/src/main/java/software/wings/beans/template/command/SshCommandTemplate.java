package software.wings.beans.template.command;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.command.AbstractCommandUnitYaml;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.ReferencedTemplate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

@OwnedBy(CDC)
@JsonTypeName("SSH")
@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
public class SshCommandTemplate implements BaseTemplate {
  private CommandType commandType;
  @JsonInclude(NON_NULL) private transient List<AbstractCommandUnitYaml> commands;
  @Wither private List<CommandUnit> commandUnits;
  @Wither @JsonIgnore private List<ReferencedTemplate> referencedTemplateList;
}
