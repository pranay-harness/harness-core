package software.wings.graphql.schema.mutation.secretManager;

import software.wings.graphql.schema.type.secrets.QLUsageScope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLUpdateHashicorpVaultInput {
  String name;
  QLHashicorpVaultAuthDetails authDetails;
  Boolean isReadOnly;
  Long secretEngineRenewalInterval;
  Boolean isDefault;
  QLUsageScope usageScope;
}
