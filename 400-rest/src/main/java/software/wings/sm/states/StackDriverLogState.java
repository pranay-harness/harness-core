/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static software.wings.common.VerificationConstants.KUBERNETES_HOSTNAME;
import static software.wings.common.VerificationConstants.STACKDRIVER_DEFAULT_LOG_MESSAGE_FIELD;
import static software.wings.common.VerificationConstants.STACK_DRIVER_DEFAULT_HOSTNAME;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.api.DeploymentType;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.AnalysisToleranceProvider;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import com.github.reinert.jjschema.Attributes;
import java.util.Set;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

@Slf4j
@FieldNameConstants(innerTypeName = "StackDriverLogStateKeys")
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class StackDriverLogState extends AbstractLogAnalysisState {
  @Attributes(required = true, title = "GCP account") private String analysisServerConfigId;

  @Attributes(title = "Region") @DefaultValue("us-central1") private String region = "us-central1";

  @Attributes(title = "LogMessageField") @DefaultValue("textPayload") private String messageField = "textPayload";

  public StackDriverLogState(String name) {
    super(name, StateType.STACK_DRIVER_LOG.name());
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  @Attributes(required = true, title = "Hostname Field")
  public String getHostnameField() {
    return hostnameField;
  }

  public void setHostnameField(String hostnameField) {
    this.hostnameField = hostnameField;
  }

  public String getMessageField() {
    if (isEmpty(messageField)) {
      return STACKDRIVER_DEFAULT_LOG_MESSAGE_FIELD;
    }
    return messageField;
  }

  public void setMessageField(String messageField) {
    this.messageField = messageField;
  }

  @Override
  protected String triggerAnalysisDataCollection(
      ExecutionContext context, VerificationStateAnalysisExecutionData executionData, Set<String> hosts) {
    return null;
  }

  @Override
  @EnumData(enumDataProvider = AnalysisToleranceProvider.class)
  @Attributes(required = true, title = "Algorithm Sensitivity")
  @DefaultValue("MEDIUM")
  public AnalysisTolerance getAnalysisTolerance() {
    if (isEmpty(tolerance)) {
      return AnalysisTolerance.LOW;
    }
    return AnalysisTolerance.valueOf(tolerance);
  }

  @Override
  @EnumData(enumDataProvider = AnalysisComparisonStrategyProvider.class)
  @Attributes(required = true, title = "Baseline for Risk Analysis")
  @DefaultValue("COMPARE_WITH_PREVIOUS")
  public AnalysisComparisonStrategy getComparisonStrategy() {
    if (isEmpty(comparisonStrategy)) {
      return AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
    }
    return AnalysisComparisonStrategy.valueOf(comparisonStrategy);
  }

  @Override
  public Logger getLogger() {
    return log;
  }

  @Override
  public String getAnalysisServerConfigId() {
    return analysisServerConfigId;
  }

  @Override
  public void setAnalysisServerConfigId(String analysisServerConfigId) {
    this.analysisServerConfigId = analysisServerConfigId;
  }

  @Override
  @Attributes(title = "Analysis Time duration (in minutes)", description = "Default 15 minutes")
  @DefaultValue("15")
  public String getTimeDuration() {
    if (isEmpty(timeDuration)) {
      return String.valueOf(15);
    }
    return timeDuration;
  }

  @Override
  @Attributes(required = true, title = "Search Keywords")
  @DefaultValue("*exception*")
  public String getQuery() {
    return query;
  }

  @Override
  public String getHostnameField(ExecutionContext executionContext) {
    if (hostnameField != null) {
      return hostnameField;
    }
    if (getDeploymentType(executionContext) == DeploymentType.KUBERNETES) {
      return KUBERNETES_HOSTNAME;
    } else {
      return STACK_DRIVER_DEFAULT_HOSTNAME;
    }
  }
}
