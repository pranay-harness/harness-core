package io.harness.pms.barriers.service;

import io.harness.pms.barriers.beans.BarrierSetupInfo;

import java.util.List;

public interface PMSBarrierService {
  List<BarrierSetupInfo> getBarrierSetupInfoList(String yaml);
}
