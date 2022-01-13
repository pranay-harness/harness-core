/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc;

import software.wings.verification.HeatMap;

import java.util.List;

/**
 * @author Vaibhav Tulsyan
 * 11/Oct/2018
 */
public interface CVDashboardService {
  List<HeatMap> getHeatMap(String accountId, String appId, int resolution, String startTime, String endTime);
}
