package io.harness.ccm.views.service;

import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ViewCustomField;
import io.harness.ccm.views.entities.ViewField;

import java.util.List;

public interface ViewCustomFieldService {
  ViewCustomField save(ViewCustomField viewCustomField, String cloudProviderTableName);

  List<ViewField> getCustomFields(String accountId);

  List<ViewField> getCustomFieldsPerView(String viewId, String accountId);

  ViewCustomField get(String uuid);

  ViewCustomField update(ViewCustomField viewCustomField, String cloudProviderTableName);

  boolean validate(ViewCustomField viewCustomField, String cloudProviderTableName);

  boolean delete(String uuid, String accountId, CEView ceView);

  boolean deleteByViewId(String viewId, String accountId);
}
