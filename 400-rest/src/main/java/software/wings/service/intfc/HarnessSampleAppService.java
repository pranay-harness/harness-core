/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc;

import software.wings.beans.Application;
import software.wings.beans.SampleAppStatus;

public interface HarnessSampleAppService {
  SampleAppStatus getSampleAppHealth(String accountId, String deploymentType);
  Application restoreSampleApp(String accountId, String deploymentType);
}
