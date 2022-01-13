/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.verification;

import static io.harness.annotations.dev.HarnessTeam.CV;

import static java.lang.Boolean.parseBoolean;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.yaml.YamlConstants;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.yaml.BaseEntityYaml;

import com.google.common.base.Preconditions;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._955_CG_YAML)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CV)
public abstract class CVConfigurationYaml extends BaseEntityYaml {
  private String connectorName;
  private String serviceName;
  private AnalysisTolerance analysisTolerance;
  private boolean enabled24x7;
  private double alertThreshold;
  private int numOfOccurrencesForAlert = 1;
  private Date snoozeStartTime;
  private Date snoozeEndTime;
  private boolean alertEnabled;

  // TODO: Remove the below setters when DX-574 is fixed.
  public void setEnabled24x7(Object value) {
    String enabled24x7 = String.valueOf(value).toLowerCase();
    Preconditions.checkArgument(YamlConstants.ALLOWED_BOOLEAN_VALUES.contains(enabled24x7),
        "Allowed values for enabled24x7 are: " + YamlConstants.ALLOWED_BOOLEAN_VALUES);
    this.enabled24x7 = parseBoolean(enabled24x7);
  }

  public void setAlertEnabled(Object value) {
    String alertEnabledStringValue = String.valueOf(value).toLowerCase();
    Preconditions.checkArgument(YamlConstants.ALLOWED_BOOLEAN_VALUES.contains(alertEnabledStringValue),
        "Allowed values for enabled24x7 are: " + YamlConstants.ALLOWED_BOOLEAN_VALUES);
    this.alertEnabled = parseBoolean(alertEnabledStringValue);
  }
}
