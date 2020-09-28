package io.harness.ccm.views.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.views.dao.CEViewDao;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.graphql.QLCEView;
import io.harness.ccm.views.service.CEViewService;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class CEViewServiceImpl implements CEViewService {
  @Inject private CEViewDao ceViewDao;

  private static final String VIEW_NAME_DUPLICATE_EXCEPTION = "View with given name already exists";

  @Override
  public CEView save(CEView ceView) {
    validateView(ceView);
    ceViewDao.save(ceView);
    return ceView;
  }

  public boolean validateView(CEView ceView) {
    CEView savedCEView = ceViewDao.findByName(ceView.getAccountId(), ceView.getName());
    if (null != savedCEView) {
      throw new InvalidRequestException(VIEW_NAME_DUPLICATE_EXCEPTION);
    }
    return true;
  }
  @Override
  public List<QLCEView> getAllViews(String accountId) {
    List<CEView> viewList = ceViewDao.findByAccountId(accountId);
    List<QLCEView> graphQLViewObjList = new ArrayList<>();
    for (CEView view : viewList) {
      graphQLViewObjList.add(QLCEView.builder()
                                 .id(view.getUuid())
                                 .name(view.getName())
                                 .createdAt(view.getCreatedAt())
                                 .lastUpdatedAt(view.getLastUpdatedAt())
                                 .chartType(view.getViewVisualization().getChartType())
                                 .viewType(view.getViewType())
                                 .build());
    }
    return graphQLViewObjList;
  }
}
