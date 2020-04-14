package software.wings.beans.template.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.Variable;
import software.wings.beans.template.BaseTemplate;

import java.util.List;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportedCommandVersion {
  private String commandStoreId;
  private String commandId;
  private String templateId;
  private String version;
  private String description;
  private String yamlContent;
  private BaseTemplate templateObject;
  private List<Variable> variables;
  private String createdAt;
  private String createdBy;
}
