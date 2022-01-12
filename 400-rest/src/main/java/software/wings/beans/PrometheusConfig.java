/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;

import software.wings.annotation.EncryptableSetting;
import software.wings.audit.ResourceType;
import software.wings.security.UsageRestrictions;
import software.wings.settings.SettingValue;
import software.wings.sm.StateType;
import software.wings.sm.states.APMVerificationState;
import software.wings.yaml.setting.VerificationProviderYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by rsingh on 3/15/18.
 */
@Data
@JsonTypeName("PROMETHEUS")
@Builder
@EqualsAndHashCode(callSuper = false)
public class PrometheusConfig extends SettingValue implements EncryptableSetting, ExecutionCapabilityDemander {
  public static final String VALIDATION_URL = "api/v1/query?query=up";
  @Attributes(title = "URL", required = true) private String url;

  @SchemaIgnore @NotEmpty private String accountId;

  public PrometheusConfig() {
    super(StateType.PROMETHEUS.name());
  }

  public PrometheusConfig(String url, String accountId) {
    this();
    this.url = url;
    this.accountId = accountId;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return Arrays.asList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(url, maskingEvaluator));
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.VERIFICATION_PROVIDER.name();
  }

  public APMValidateCollectorConfig createAPMValidateCollectorConfig() {
    return createAPMValidateCollectorConfig(VALIDATION_URL);
  }

  public APMValidateCollectorConfig createAPMValidateCollectorConfig(String urlToFetch) {
    return APMValidateCollectorConfig.builder()
        .baseUrl(url)
        .url(urlToFetch)
        .collectionMethod(APMVerificationState.Method.GET)
        .headers(new HashMap<>())
        .options(new HashMap<>())
        .build();
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class PrometheusYaml extends VerificationProviderYaml {
    private String prometheusUrl;

    @Builder
    public PrometheusYaml(
        String type, String harnessApiVersion, String prometheusUrl, UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.prometheusUrl = prometheusUrl;
    }
  }
}
