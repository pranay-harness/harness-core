/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.config.CCMConfig;
import io.harness.ccm.config.CloudCostAware;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import io.harness.expression.ExpressionEvaluator;

import software.wings.annotation.EncryptableSetting;
import software.wings.audit.ResourceType;
import software.wings.jersey.JsonViews;
import software.wings.security.UsageRestrictions;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingVariableTypes;
import software.wings.yaml.setting.CloudProviderYaml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;

@JsonTypeName("AWS")
@Data
@Builder
@ToString(exclude = "secretKey")
@EqualsAndHashCode(callSuper = false)
@OwnedBy(CDP)
@TargetModule(HarnessModule._970_API_SERVICES_BEANS)
public class AwsConfig extends SettingValue implements EncryptableSetting, CloudCostAware {
  private static final String AWS_URL = "https://aws.amazon.com/";
  @Attributes(title = "Access Key") @Encrypted(fieldName = "access_key", isReference = true) private char[] accessKey;
  @Attributes(title = "Secret Key") @Encrypted(fieldName = "secret_key") private char[] secretKey;
  @SchemaIgnore @NotEmpty private String accountId; // internal
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedSecretKey;

  @Attributes(title = "Use Ec2 Iam role") private boolean useEc2IamCredentials;
  @Attributes(title = "Ec2 Iam role tags") private String tag;
  @JsonInclude(Include.NON_NULL) @SchemaIgnore private CCMConfig ccmConfig;
  private boolean useIRSA;
  private boolean assumeCrossAccountRole;
  private AwsCrossAccountAttributes crossAccountAttributes;
  private String defaultRegion;

  @Attributes(title = "Use Encrypted Access Key") private boolean useEncryptedAccessKey;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedAccessKey;

  public AwsConfig() {
    super(SettingVariableTypes.AWS.name());
  }

  public AwsConfig(char[] accessKey, char[] secretKey, String accountId, String encryptedSecretKey,
      boolean useEc2IamCredentials, String tag, CCMConfig ccmConfig, boolean useIRSA, boolean assumeCrossAccountRole,
      AwsCrossAccountAttributes crossAccountAttributes, String defaultRegion, boolean useEncryptedAccessKey,
      String encryptedAccessKey) {
    this();
    this.accessKey = accessKey == null ? null : accessKey.clone();
    this.secretKey = secretKey == null ? null : secretKey.clone();
    this.accountId = accountId;
    this.encryptedSecretKey = encryptedSecretKey;
    this.useEc2IamCredentials = useEc2IamCredentials;
    this.tag = tag;
    this.ccmConfig = ccmConfig;
    this.useIRSA = useIRSA;
    this.assumeCrossAccountRole = assumeCrossAccountRole;
    this.crossAccountAttributes = crossAccountAttributes;
    this.defaultRegion = defaultRegion;
    this.useEncryptedAccessKey = useEncryptedAccessKey;
    this.encryptedAccessKey = encryptedAccessKey;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();

    executionCapabilities.add(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(AWS_URL, maskingEvaluator));

    if ((this.isUseEc2IamCredentials() || this.useIRSA) && StringUtils.isNotEmpty(tag)) {
      executionCapabilities.add(SelectorCapability.builder()
                                    .selectors(new HashSet<String>(Arrays.asList(tag)))
                                    .selectorOrigin("Cloud Provider")
                                    .build());
    }

    return executionCapabilities;
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.CLOUD_PROVIDER.name();
  }

  @Override
  public List<String> fetchRelevantEncryptedSecrets() {
    if (useEc2IamCredentials || useIRSA) {
      return Collections.emptyList();
    }

    return useEncryptedAccessKey ? Arrays.asList(encryptedAccessKey, encryptedSecretKey)
                                 : Collections.singletonList(encryptedSecretKey);
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends CloudProviderYaml {
    private String accessKey;
    private String accessKeySecretId;
    private String secretKey;
    private boolean useEc2IamCredentials;
    private String tag;
    private boolean useIRSA;
    private boolean assumeCrossAccountRole;
    private AwsCrossAccountAttributes crossAccountAttributes;
    private String defaultRegion;

    @Builder
    public Yaml(String type, String harnessApiVersion, String accessKey, String accessKeySecretId, String secretKey,
        UsageRestrictions.Yaml usageRestrictions, boolean useEc2IamCredentials, String tag, boolean useIRSA,
        boolean assumeCrossAccountRole, AwsCrossAccountAttributes crossAccountAttributes, String defaultRegion) {
      super(type, harnessApiVersion, usageRestrictions);
      this.accessKey = accessKey;
      this.accessKeySecretId = accessKeySecretId;
      this.secretKey = secretKey;
      this.useEc2IamCredentials = useEc2IamCredentials;
      this.tag = tag;
      this.useIRSA = useIRSA;
      this.assumeCrossAccountRole = assumeCrossAccountRole;
      this.crossAccountAttributes = crossAccountAttributes;
      this.defaultRegion = defaultRegion;
    }
  }
}
