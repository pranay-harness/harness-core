package io.harness.mongo;

import com.google.inject.Singleton;
import io.harness.annotations.dev.OwnedBy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Singleton
public class MongoSSLConfig {
  private boolean enabled;
  private String trustStorePath;
  private String trustStorePassword;
}
