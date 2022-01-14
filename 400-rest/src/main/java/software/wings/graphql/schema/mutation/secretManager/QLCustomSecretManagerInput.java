package software.wings.graphql.schema.mutation.secretManager;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLCustomSecretManagerInput extends QLSecretManagerInput {
  String name;
  String templateId;
  Set<String> delegateSelectors;
  Set<QLEncryptedDataParams> testVariables;
  boolean executeOnDelegate;
  boolean isConnectorTemplatized;
  String host;
  String commandPath;
  String connectorId;
}
