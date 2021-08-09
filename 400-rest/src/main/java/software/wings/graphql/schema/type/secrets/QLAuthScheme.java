package software.wings.graphql.schema.type.secrets;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
@OwnedBy(CDP) @TargetModule(HarnessModule._380_CG_GRAPHQL) public enum QLAuthScheme { NTLM, BASIC, KERBEROS }
