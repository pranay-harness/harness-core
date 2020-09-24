package io.harness.ccm.views.dao;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.CEView.CEViewKeys;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CEViewDao {
  @Inject private HPersistence hPersistence;

  public boolean save(CEView ceView) {
    return hPersistence.save(ceView) != null;
  }

  public CEView findByName(String accountId, String name) {
    return hPersistence.createQuery(CEView.class)
        .filter(CEViewKeys.accountId, accountId)
        .filter(CEViewKeys.name, name)
        .get();
  }
}
