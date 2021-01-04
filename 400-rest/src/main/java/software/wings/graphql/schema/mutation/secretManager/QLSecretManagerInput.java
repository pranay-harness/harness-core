package software.wings.graphql.schema.mutation.secretManager;

import software.wings.graphql.schema.type.secrets.QLUsageScope;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class QLSecretManagerInput {
  private boolean isDefault;
  private QLUsageScope usageScope;
}
