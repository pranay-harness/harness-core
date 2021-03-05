package software.wings.graphql.schema.mutation.connector.input.docker;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.utils.RequestField;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonIgnoreProperties(ignoreUnknown = true)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLDockerConnectorInput {
  private RequestField<String> name;

  private RequestField<String> userName;
  private RequestField<String> URL;
  private RequestField<String> passwordSecretId;
  private RequestField<List<String>> delegateSelectors;
}
