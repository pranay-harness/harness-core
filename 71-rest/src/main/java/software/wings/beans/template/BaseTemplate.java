package software.wings.beans.template;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import software.wings.beans.template.command.HttpTemplate;
import software.wings.beans.template.command.SshCommandTemplate;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = EXTERNAL_PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = SshCommandTemplate.class, name = "SSH")
  , @JsonSubTypes.Type(value = HttpTemplate.class, name = "HTTP")
})
public interface BaseTemplate {}
