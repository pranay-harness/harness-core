package software.wings.service.intfc;

import software.wings.beans.Permit;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 7/17/18.
 */
public interface PermitService {
  String acquirePermit(@Valid Permit permit);
  boolean releasePermit(@NotNull String permitId);
}
