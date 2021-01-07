package io.harness.ccm.views.service;

import io.harness.ccm.views.entities.CEView;
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
  List<QLCEView> getAllViews(String accountId);
  List<CEView> getViewByState(String accountId, ViewState viewState);
}
