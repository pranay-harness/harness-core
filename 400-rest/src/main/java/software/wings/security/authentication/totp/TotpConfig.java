package software.wings.security.authentication.totp;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Singleton
public class TotpConfig {
  private String secOpsEmail;
  private int incorrectAttemptsUntilSecOpsNotified;
  private TotpLimit limit;
}
