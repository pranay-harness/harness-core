package software.wings.app;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
@Singleton
public class JobsFrequencyConfig {
  private long accountLicenseCheckJobFrequencyInMinutes;
  private long accountBackgroundJobFrequencyInMinutes;
  private long accountDeletionJobFrequencyInMinutes;
}
