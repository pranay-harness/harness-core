package software.wings.scheduler;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@TargetModule(HarnessModule._360_CG_MANAGER)
@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapGroupSyncConfig {
  int threadPoolSize;
  long intervalInMinutes;
  long targetIntervalInMinutes;
  long acceptableNoAlertDelayInMinutes;
}
