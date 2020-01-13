package software.wings.beans.template;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static software.wings.common.TemplateConstants.ARTIFACT_SOURCE;
import static software.wings.common.TemplateConstants.HTTP;
import static software.wings.common.TemplateConstants.SHELL_SCRIPT;
import static software.wings.common.TemplateConstants.SSH;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import software.wings.beans.template.artifactsource.ArtifactSourceTemplate;
import software.wings.beans.template.command.HttpTemplate;
import software.wings.beans.template.command.ShellScriptTemplate;
import software.wings.beans.template.command.SshCommandTemplate;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = EXTERNAL_PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = SshCommandTemplate.class, name = SSH)
  , @JsonSubTypes.Type(value = HttpTemplate.class, name = HTTP),
      @JsonSubTypes.Type(value = ShellScriptTemplate.class, name = SHELL_SCRIPT),
      @JsonSubTypes.Type(value = ArtifactSourceTemplate.class, name = ARTIFACT_SOURCE)
})
public interface BaseTemplate {}
