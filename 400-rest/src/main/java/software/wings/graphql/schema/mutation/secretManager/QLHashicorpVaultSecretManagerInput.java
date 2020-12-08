package software.wings.graphql.schema.mutation.secretManager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLHashicorpVaultSecretManagerInput extends QLSecretManagerInput {
  String name;
  String vaultUrl;
  QLHashicorpVaultAuthDetails authDetails;
  String basePath;
  boolean isReadOnly;
  String secretEngineName;
  int secretEngineVersion;
  long secretEngineRenewalInterval;
}
