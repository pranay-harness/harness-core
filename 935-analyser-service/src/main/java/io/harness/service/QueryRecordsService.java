package io.harness.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.event.QueryRecordEntity;
import io.harness.service.beans.QueryRecordKey;

import java.util.List;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public interface QueryRecordsService {
  Map<QueryRecordKey, List<QueryRecordEntity>> findAllEntries();
}
