package io.harness.ccm.views.service;

import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.graphql.QLCEView;

import java.util.List;

public interface CEViewService {
  CEView save(CEView ceView);
  CEView get(String uuid);
  CEView update(CEView ceView);
  boolean delete(String uuid, String accountId);
  List<QLCEView> getAllViews(String accountId);
}
