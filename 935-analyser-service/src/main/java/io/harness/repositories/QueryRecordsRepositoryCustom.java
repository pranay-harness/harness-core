package io.harness.repositories;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.event.QueryRecordEntity;

import java.util.List;

@OwnedBy(HarnessTeam.PIPELINE)
public interface QueryRecordsRepositoryCustom {
  List<QueryRecordEntity> findAllHashes(int page, int size);
}
