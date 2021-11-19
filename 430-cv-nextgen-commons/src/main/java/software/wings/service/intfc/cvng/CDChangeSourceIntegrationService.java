package software.wings.service.intfc.cvng;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.change.HarnessCDCurrentGenEventMetadata;

import java.time.Instant;
import java.util.List;

@OwnedBy(CV)
public interface CDChangeSourceIntegrationService {
  List<HarnessCDCurrentGenEventMetadata> getCurrentGenEventsBetween(
      String accountId, String appId, String serviceId, String environmentId, Instant startTime, Instant endTime);
}
