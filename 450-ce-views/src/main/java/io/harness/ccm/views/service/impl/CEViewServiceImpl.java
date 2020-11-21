package io.harness.ccm.views.service.impl;

import io.harness.ccm.views.dao.CEViewDao;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ViewChartType;
import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.entities.ViewState;
import io.harness.ccm.views.entities.ViewTimeGranularity;
import io.harness.ccm.views.entities.ViewVisualization;
import io.harness.ccm.views.graphql.QLCEView;
import io.harness.ccm.views.service.CEViewService;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CEViewServiceImpl implements CEViewService {
  @Inject private CEViewDao ceViewDao;

  private static final String VIEW_NAME_DUPLICATE_EXCEPTION = "View with given name already exists";
  private static final String VIEW_LIMIT_REACHED_EXCEPTION = "Maximum allowed custom views limit(50) has been reached";
  private static final int VIEW_COUNT = 50;
  @Override
  public CEView save(CEView ceView) {
    validateView(ceView);
    ceView.setViewState(ViewState.DRAFT);
    ceView.setUuid(null);
    ceViewDao.save(ceView);
    return ceView;
  }

  public boolean validateView(CEView ceView) {
    CEView savedCEView = ceViewDao.findByName(ceView.getAccountId(), ceView.getName());
    if (null != savedCEView) {
      throw new InvalidRequestException(VIEW_NAME_DUPLICATE_EXCEPTION);
    }
    List<CEView> views = ceViewDao.findByAccountId(ceView.getAccountId());
    if (views.size() >= VIEW_COUNT) {
      throw new InvalidRequestException(VIEW_LIMIT_REACHED_EXCEPTION);
    }
    if (ceView.getViewVisualization() == null) {
      ceView.setViewVisualization(ViewVisualization.builder()
                                      .granularity(ViewTimeGranularity.DAY)
                                      .chartType(ViewChartType.STACKED_TIME_SERIES)
                                      .groupBy(ViewField.builder()
                                                   .fieldId("product")
                                                   .fieldName("Product")
                                                   .identifier(ViewFieldIdentifier.COMMON)
                                                   .identifierName(ViewFieldIdentifier.COMMON.getDisplayName())
                                                   .build())
                                      .build());
    }
    return true;
  }

  @Override
  public CEView get(String uuid) {
    CEView view = ceViewDao.get(uuid);
    if (view.getViewRules() == null) {
      view.setViewRules(Collections.emptyList());
    }
    return view;
  }

  @Override
  public CEView update(CEView ceView) {
    return ceViewDao.update(ceView);
  }

  @Override
  public boolean delete(String uuid, String accountId) {
    return ceViewDao.delete(uuid, accountId);
  }

  @Override
  public List<QLCEView> getAllViews(String accountId) {
    List<CEView> viewList = ceViewDao.findByAccountId(accountId);
    List<QLCEView> graphQLViewObjList = new ArrayList<>();
    for (CEView view : viewList) {
      ViewChartType vChartType = null;
      if (view.getViewVisualization() != null) {
        // For DRAFT support, no visualizations have been set at this point
        vChartType = view.getViewVisualization().getChartType();
      }
      graphQLViewObjList.add(QLCEView.builder()
                                 .id(view.getUuid())
                                 .name(view.getName())
                                 .createdAt(view.getCreatedAt())
                                 .lastUpdatedAt(view.getLastUpdatedAt())
                                 .chartType(vChartType)
                                 .viewType(view.getViewType())
                                 .viewState(view.getViewState())
                                 .build());
    }
    return graphQLViewObjList;
  }
}
