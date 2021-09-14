/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.views.service;

import io.harness.ccm.views.dto.DefaultViewIdDto;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.entities.ViewState;
import io.harness.ccm.views.graphql.QLCEView;

import com.google.cloud.bigquery.BigQuery;
import java.util.List;

public interface CEViewService {
  CEView save(CEView ceView);
  CEView get(String uuid);
  CEView update(CEView ceView);
  CEView updateTotalCost(CEView ceView, BigQuery bigQuery, String cloudProviderTableName);
  boolean delete(String uuid, String accountId);
  List<QLCEView> getAllViews(String accountId, boolean includeDefault);
  List<CEView> getViewByState(String accountId, ViewState viewState);
  void createDefaultView(String accountId, ViewFieldIdentifier viewFieldIdentifier);
  DefaultViewIdDto getDefaultViewIds(String accountId);
}
