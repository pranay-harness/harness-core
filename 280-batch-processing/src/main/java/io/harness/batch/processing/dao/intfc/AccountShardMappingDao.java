package io.harness.batch.processing.dao.intfc;

import io.harness.batch.processing.entities.AccountShardMapping;

import java.util.List;

public interface AccountShardMappingDao {
  List<AccountShardMapping> getAccountShardMapping();
}
