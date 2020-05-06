package software.wings.service.impl.ci;

import io.harness.delegate.beans.ResponseData;
import software.wings.beans.ci.CIK8BuildTaskParams;

/**
 *  Delegate tasks helper for sending tasks on behalf of CI
 */

public interface CIDelegateTaskHelperService { ResponseData setBuildEnv(CIK8BuildTaskParams cik8BuildTaskParams); }
