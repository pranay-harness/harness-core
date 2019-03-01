package software.wings.service.intfc;

import software.wings.beans.Application;
import software.wings.beans.SampleAppStatus;

public interface HarnessSampleAppService {
  SampleAppStatus getSampleAppHealth(String accountId, String deploymentType);
  Application restoreSampleApp(String accountId, String deploymentType, Application application);
}
