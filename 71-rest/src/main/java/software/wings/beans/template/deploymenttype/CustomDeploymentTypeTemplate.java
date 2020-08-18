package software.wings.beans.template.deploymenttype;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static software.wings.common.TemplateConstants.CUSTOM_DEPLOYMENT_TYPE;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.template.BaseTemplate;

import java.util.Map;

@Value
@Builder
@JsonInclude(NON_NULL)
@JsonTypeName(CUSTOM_DEPLOYMENT_TYPE)
@OwnedBy(HarnessTeam.CDP)
public class CustomDeploymentTypeTemplate implements BaseTemplate {
  private String fetchInstanceScript;
  private String hostObjectArrayPath;
  private Map<String, String> hostAttributes;
}
