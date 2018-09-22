/**
 *
 */

package software.wings.sm;

import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Builder
@Value
public class ExecutionInterruptEffect {
  private String interruptId;
  private Date tookEffectAt;
}
