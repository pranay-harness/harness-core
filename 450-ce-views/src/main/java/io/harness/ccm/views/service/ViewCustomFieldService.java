/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.views.service;

import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ViewCustomField;
import io.harness.ccm.views.entities.ViewField;

import com.google.cloud.bigquery.BigQuery;
import java.util.List;

public interface ViewCustomFieldService {
  ViewCustomField save(ViewCustomField viewCustomField, BigQuery bigQuery, String cloudProviderTableName);

  List<ViewField> getCustomFields(String accountId);

  List<ViewField> getCustomFieldsPerView(String viewId, String accountId);

  ViewCustomField get(String uuid);

  ViewCustomField update(ViewCustomField viewCustomField, BigQuery bigQuery, String cloudProviderTableName);

  boolean validate(ViewCustomField viewCustomField, BigQuery bigQuery, String cloudProviderTableName);

  boolean delete(String uuid, String accountId, CEView ceView);

  boolean deleteByViewId(String viewId, String accountId);
}
