package software.wings.yaml.templatelibrary;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.common.TemplateConstants.CUSTOM;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.template.artifactsource.CustomRepositoryMapping;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(CDC)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("CUSTOM")
@JsonPropertyOrder({"harnessApiVersion"})
@TargetModule(Module._870_CG_YAML_BEANS)
public class CustomArtifactSourceTemplateYaml extends ArtifactSourceTemplateYaml {
  private String script;
  private String timeout;
  private CustomRepositoryMapping customRepositoryMapping;

  @Builder
  public CustomArtifactSourceTemplateYaml(String script, String timeout,
      CustomRepositoryMapping customRepositoryMapping, String type, String harnessApiVersion, String description,
      List<TemplateVariableYaml> templateVariableYamlList) {
    super(type, harnessApiVersion, description, templateVariableYamlList, CUSTOM);
    this.script = script;
    this.customRepositoryMapping = customRepositoryMapping;
    this.timeout = timeout;
  }

  public CustomArtifactSourceTemplateYaml() {
    super(CUSTOM);
  }
}
