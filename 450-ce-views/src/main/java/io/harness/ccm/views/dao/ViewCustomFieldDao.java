package io.harness.ccm.views.dao;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.views.entities.ViewCustomField;
import io.harness.ccm.views.entities.ViewCustomField.ViewCustomFieldKeys;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Singleton
public class ViewCustomFieldDao {
  @Inject private HPersistence hPersistence;

  public boolean save(ViewCustomField viewCustomField) {
    return hPersistence.save(viewCustomField) != null;
  }

  public ViewCustomField findByName(String accountId, String viewId, String name) {
    return hPersistence.createQuery(ViewCustomField.class)
        .filter(ViewCustomFieldKeys.accountId, accountId)
        .filter(ViewCustomFieldKeys.viewId, viewId)
        .filter(ViewCustomFieldKeys.name, name)
        .get();
  }

  public List<ViewCustomField> findByAccountId(String accountId) {
    return hPersistence.createQuery(ViewCustomField.class).filter(ViewCustomFieldKeys.accountId, accountId).asList();
  }

  public ViewCustomField getById(String uuid) {
    return hPersistence.createQuery(ViewCustomField.class).filter(ViewCustomFieldKeys.uuid, uuid).get();
  }
}
