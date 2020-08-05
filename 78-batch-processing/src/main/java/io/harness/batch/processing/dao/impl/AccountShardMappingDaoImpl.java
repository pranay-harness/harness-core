package io.harness.batch.processing.dao.impl;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.batch.processing.dao.intfc.AccountShardMappingDao;
import io.harness.batch.processing.entities.AccountShardMapping;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Slf4j
public class AccountShardMappingDaoImpl implements AccountShardMappingDao {
  @Autowired @Inject private HPersistence hPersistence;

  @Override
  public List<AccountShardMapping> getAccountShardMapping() {
    return hPersistence.createQuery(AccountShardMapping.class, excludeAuthority).asList();
  }
}
