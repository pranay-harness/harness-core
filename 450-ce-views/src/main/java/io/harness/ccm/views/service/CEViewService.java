package io.harness.ccm.views.service;

import io.harness.ccm.views.dto.DefaultViewIdDto;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.entities.ViewState;
import io.harness.ccm.views.graphql.QLCEView;

import java.util.List;

public interface CEViewService {
  CEView save(CEView ceView);

  double getActualCostForPerspectiveBudget(String accountId, String perspectiveId);

  CEView get(String uuid);
  CEView update(CEView ceView);
  CEView updateTotalCost(CEView ceView, String cloudProviderTableName);
  boolean delete(String uuid, String accountId);
  List<QLCEView> getAllViews(String accountId, boolean includeDefault);
  List<CEView> getViewByState(String accountId, ViewState viewState);
  void createDefaultView(String accountId, ViewFieldIdentifier viewFieldIdentifier);
  DefaultViewIdDto getDefaultViewIds(String accountId);

  Double getLastMonthCostForPerspective(String accountId, String perspectiveId);
  Double getForecastCostForPerspective(String accountId, String perspectiveId);
}
