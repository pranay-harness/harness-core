package software.wings.service.intfc;

import software.wings.beans.Permit;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public interface PermitService {
  String acquirePermit(@Valid Permit permit);
  boolean releasePermitByKey(@NotNull String key);
}
