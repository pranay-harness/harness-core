package software.wings.scheduler;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 7/17/18.
 */
public interface PermitService {
  String acquirePermit(@Valid Permit permit);
  boolean releasePermit(@NotNull String permitId);
}
