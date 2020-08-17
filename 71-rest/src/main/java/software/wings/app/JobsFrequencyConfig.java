package software.wings.app;

import com.google.inject.Singleton;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Singleton
public class JobsFrequencyConfig {
  private long accountLicenseCheckJobFrequencyInMinutes;
  private long accountBackgroundJobFrequencyInMinutes;
  private long accountDeletionJobFrequencyInMinutes;
}
