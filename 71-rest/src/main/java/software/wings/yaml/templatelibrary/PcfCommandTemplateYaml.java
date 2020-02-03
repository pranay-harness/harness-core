package software.wings.yaml.templatelibrary;

import static software.wings.common.TemplateConstants.PCF_PLUGIN;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(PCF_PLUGIN)
@JsonPropertyOrder({"harnessApiVersion"})
public class PcfCommandTemplateYaml extends TemplateLibraryYaml {
  private String scriptString;
  private Integer timeoutIntervalInMinutes = 5;

  @Builder
  public PcfCommandTemplateYaml(String type, String harnessApiVersion, String description, String scriptString,
      Integer timeoutIntervalInMinutes, List<TemplateVariableYaml> templateVariableYamlList) {
    super(type, harnessApiVersion, description, templateVariableYamlList);
    this.scriptString = scriptString;
    this.timeoutIntervalInMinutes = timeoutIntervalInMinutes;
  }
}
