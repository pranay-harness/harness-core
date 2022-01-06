/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.config;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import io.harness.expression.ExpressionEvaluator;

import software.wings.annotation.EncryptableSetting;
import software.wings.audit.ResourceType;
import software.wings.jersey.JsonViews;
import software.wings.security.UsageRestrictions;
import software.wings.settings.SettingValue;
import software.wings.sm.StateType;
import software.wings.yaml.setting.VerificationProviderYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by rsingh on 8/21/17.
 */
@JsonTypeName("LOGZ")
@Data
@Builder
@ToString(exclude = "token")
@EqualsAndHashCode(callSuper = false)
public class LogzConfig extends SettingValue implements EncryptableSetting, ExecutionCapabilityDemander {
  @Attributes(title = "Logz.io URL", required = true) @NotEmpty private String logzUrl;

  @Attributes(title = "Token", required = true) @Encrypted(fieldName = "token") private char[] token;

  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedToken;
  /**
   * Instantiates a new Splunk config.
   */
  public LogzConfig() {
    super(StateType.LOGZ.name());
  }

  private LogzConfig(String logzUrl, char[] token, String accountId, String encryptedToken) {
    this();
    this.logzUrl = logzUrl;
    this.token = token;
    this.accountId = accountId;
    this.encryptedToken = encryptedToken;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return Arrays.asList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(logzUrl, maskingEvaluator));
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.VERIFICATION_PROVIDER.name();
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends VerificationProviderYaml {
    private String logzUrl;
    private String token;

    @Builder
    public Yaml(
        String type, String harnessApiVersion, String logzUrl, String token, UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.logzUrl = logzUrl;
      this.token = token;
    }
  }
}
