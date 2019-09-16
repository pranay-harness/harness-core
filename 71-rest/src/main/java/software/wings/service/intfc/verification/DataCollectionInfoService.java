package software.wings.service.intfc.verification;

import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.verification.CVConfiguration;

import java.time.Instant;

public interface DataCollectionInfoService {
  DataCollectionInfoV2 create(CVConfiguration cvConfiguration, Instant startTime, Instant endTime);
}
