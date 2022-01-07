/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
