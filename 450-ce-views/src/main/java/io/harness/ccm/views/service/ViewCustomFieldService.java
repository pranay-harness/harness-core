package io.harness.ccm.views.service;

import io.harness.ccm.views.entities.ViewCustomField;
import io.harness.ccm.views.entities.ViewField;

import java.util.List;

public interface ViewCustomFieldService {
  ViewCustomField save(ViewCustomField viewCustomField);

  List<ViewField> getCustomFields(String accountId);

  ViewCustomField get(String uuid);
}
