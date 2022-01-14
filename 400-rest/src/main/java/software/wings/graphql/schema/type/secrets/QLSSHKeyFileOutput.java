package software.wings.graphql.schema.type.secrets;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLObject;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(PL)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLSSHKeyFileOutput implements QLObject {
  String path;
  String passphraseSecretId;
}
