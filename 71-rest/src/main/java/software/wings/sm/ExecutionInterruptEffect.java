/**
 *
 */

package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@OwnedBy(CDC)
@Value
@Builder
public class ExecutionInterruptEffect {
  private String interruptId;
  private Date tookEffectAt;
}
