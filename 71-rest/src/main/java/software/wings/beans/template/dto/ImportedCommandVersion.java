package software.wings.beans.template.dto;

import software.wings.beans.Variable;
import software.wings.beans.template.BaseTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportedCommandVersion {
  private String commandStoreName;
  private String commandName;
  private String commandDisplayName;
  private String templateId;
  private String version;
  private String description;
  private String yamlContent;
  private BaseTemplate templateObject;
  private List<Variable> variables;
  private String createdAt;
  private String createdBy;
}
