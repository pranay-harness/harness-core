/**
 *
 */

package software.wings.sm;

import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder
public class ExecutionInterruptEffect {
  private String interruptId;
  private Date tookEffectAt;
}
