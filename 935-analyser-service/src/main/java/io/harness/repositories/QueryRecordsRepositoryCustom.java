package io.harness.repositories;

import io.harness.event.QueryRecordEntity;

import java.util.List;

public interface QueryRecordsRepositoryCustom {
  List<QueryRecordEntity> findAllHashes(int page, int size);
}
