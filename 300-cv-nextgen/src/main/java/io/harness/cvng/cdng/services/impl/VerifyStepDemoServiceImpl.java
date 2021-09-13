package io.harness.cvng.cdng.services.impl;

import io.harness.cvng.analysis.services.api.DeploymentLogAnalysisService;
import io.harness.cvng.analysis.services.api.DeploymentTimeSeriesAnalysisService;
import io.harness.cvng.cdng.services.api.VerifyStepDemoService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VerifyStepDemoServiceImpl implements VerifyStepDemoService {
  @Inject private DeploymentTimeSeriesAnalysisService deploymentTimeSeriesAnalysisService;
  @Inject private DeploymentLogAnalysisService deploymentLogAnalysisService;

  // These methods are for generating demo template. They are for local env and can be called from
  // VerificationApplication for updating demo data template. Copy json printed in logs to template file.
  @Override
  public void createTimeSeriesDemoTemplate(String verificationTaskId) {
    log.info("Deployment timeseries demo data: "
        + deploymentTimeSeriesAnalysisService.getTimeSeriesDemoTemplate(verificationTaskId));
  }

  @Override
  public void createLogsDemoTemplate(String verificationTaskId) {
    log.info("Deployment log demo data: " + deploymentLogAnalysisService.getLogDemoTemplate(verificationTaskId));
  }
}
