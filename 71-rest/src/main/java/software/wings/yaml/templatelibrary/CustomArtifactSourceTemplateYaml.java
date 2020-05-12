package software.wings.yaml.templatelibrary;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static software.wings.common.TemplateConstants.CUSTOM;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.template.artifactsource.CustomRepositoryMapping;

import java.util.List;

@OwnedBy(CDC)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("CUSTOM")
@JsonPropertyOrder({"harnessApiVersion"})
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
