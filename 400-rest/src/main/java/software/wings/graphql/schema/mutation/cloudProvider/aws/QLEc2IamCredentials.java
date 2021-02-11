package software.wings.graphql.schema.mutation.cloudProvider.aws;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.utils.RequestField;

import software.wings.graphql.schema.type.secrets.QLUsageScope;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonIgnoreProperties(ignoreUnknown = true)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLEc2IamCredentials {
  private RequestField<String> delegateSelector;
  private RequestField<QLUsageScope> usageScope;
}
