package io.harness.service;

import io.harness.event.QueryRecordEntity;
import io.harness.service.beans.QueryRecordKey;

import java.util.List;
import java.util.Map;

public interface QueryRecordsService {
  Map<QueryRecordKey, List<QueryRecordEntity>> findAllEntries();
}
