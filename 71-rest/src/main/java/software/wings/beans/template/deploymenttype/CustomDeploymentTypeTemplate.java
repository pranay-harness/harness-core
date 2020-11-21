package software.wings.beans.template.deploymenttype;

import static software.wings.common.TemplateConstants.CUSTOM_DEPLOYMENT_TYPE;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.template.BaseTemplate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(NON_NULL)
@JsonTypeName(CUSTOM_DEPLOYMENT_TYPE)
@OwnedBy(HarnessTeam.CDP)
public class CustomDeploymentTypeTemplate implements BaseTemplate {
  private String fetchInstanceScript;
  private String hostObjectArrayPath;
  private Map<String, String> hostAttributes;

  public CustomDeploymentTypeTemplateBuilder but() {
    return CustomDeploymentTypeTemplate.builder()
        .hostObjectArrayPath(hostObjectArrayPath)
        .hostAttributes(hostAttributes)
        .fetchInstanceScript(fetchInstanceScript);
  }
}
