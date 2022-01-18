package io.harness.ccm.service.impl;

import io.harness.ccm.commons.dao.anomaly.AnomalyDao;
import io.harness.ccm.service.intf.AnomalyService;

import com.google.inject.Inject;

public class AnomalyServiceImpl implements AnomalyService {
  @Inject AnomalyDao anomalyDao;
}
