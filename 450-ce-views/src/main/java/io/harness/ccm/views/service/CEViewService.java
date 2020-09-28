package io.harness.ccm.views.service;

import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.graphql.QLCEView;

import java.util.List;

public interface CEViewService {
  CEView save(CEView ceView);
  List<QLCEView> getAllViews(String accountId);
}
