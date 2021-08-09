package software.wings.helpers.ext.gcb.models;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(CDC) @TargetModule(HarnessModule._960_API_SERVICES) public enum SubstitutionOption { MUST_MATCH, ALLOW_LOOSE }
