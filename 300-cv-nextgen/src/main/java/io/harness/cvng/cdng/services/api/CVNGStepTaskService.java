/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.cdng.services.api;

import io.harness.cvng.activity.beans.DeploymentActivitySummaryDTO;
import io.harness.cvng.analysis.beans.LogAnalysisClusterChartDTO;
import io.harness.cvng.analysis.beans.LogAnalysisClusterDTO;
import io.harness.cvng.analysis.beans.TransactionMetricInfoSummaryPageDTO;
import io.harness.cvng.cdng.entities.CVNGStepTask;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.HealthSourceDTO;
import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.filterParams.DeploymentLogAnalysisFilter;
import io.harness.cvng.core.beans.params.filterParams.DeploymentTimeSeriesAnalysisFilter;
import io.harness.ng.beans.PageResponse;

import java.util.List;
import java.util.Set;

public interface CVNGStepTaskService {
  void create(CVNGStepTask cvngStepTask);
  void notifyCVNGStep(CVNGStepTask entity);
  CVNGStepTask getByCallBackId(String callBackId);
  DeploymentActivitySummaryDTO getDeploymentSummary(String callBackId);
  TransactionMetricInfoSummaryPageDTO getDeploymentActivityTimeSeriesData(String accountId, String callBackId,
      DeploymentTimeSeriesAnalysisFilter deploymentTimeSeriesAnalysisFilter, PageParams pageParams);
  Set<HealthSourceDTO> healthSources(String accountId, String callBackId);
  List<LogAnalysisClusterChartDTO> getDeploymentActivityLogAnalysisClusters(
      String accountId, String callBackId, DeploymentLogAnalysisFilter deploymentLogAnalysisFilter);
  PageResponse<LogAnalysisClusterDTO> getDeploymentActivityLogAnalysisResult(String accountId, String callBackId,
      Integer label, DeploymentLogAnalysisFilter deploymentLogAnalysisFilter, PageParams pageParams);
}
