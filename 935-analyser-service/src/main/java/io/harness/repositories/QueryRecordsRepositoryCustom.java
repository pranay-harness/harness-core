package io.harness.repositories;

import java.util.Map;
import java.util.Set;

public interface QueryRecordsRepositoryCustom {
  Map<String, Set<String>> findAllHashes(int page, int size);
}
