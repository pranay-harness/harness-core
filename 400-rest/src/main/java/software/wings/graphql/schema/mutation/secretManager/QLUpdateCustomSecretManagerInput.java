package software.wings.graphql.schema.mutation.secretManager;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.secrets.QLUsageScope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLUpdateCustomSecretManagerInput {
  String uuid;
  String name;
  String templateId;
  Set<String> delegateSelectors;
  Set<QLEncryptedDataParams> testVariables;
  boolean executeOnDelegate;
  boolean isConnectorTemplatized;
  String host;
  String commandPath;
  String connectorId;
  QLUsageScope usageScope;
}
