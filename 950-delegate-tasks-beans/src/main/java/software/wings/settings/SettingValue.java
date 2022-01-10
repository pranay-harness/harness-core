/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.settings;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.helper.SettingValueHelper;

import software.wings.security.UsageRestrictions;
import software.wings.yaml.BaseEntityYaml;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.github.reinert.jjschema.SchemaIgnore;
import java.lang.reflect.Field;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = As.EXISTING_PROPERTY)
@FieldNameConstants(innerTypeName = "SettingValueKeys")
public abstract class SettingValue implements ExecutionCapabilityDemander {
  @Getter @Setter String type;
  @JsonIgnore @SchemaIgnore private boolean isCertValidationRequired;
  @JsonIgnore @SchemaIgnore private transient boolean decrypted;

  @SchemaIgnore
  public boolean isCertValidationRequired() {
    return isCertValidationRequired;
  }

  public void setCertValidationRequired(boolean isCertValidationRequired) {
    this.isCertValidationRequired = isCertValidationRequired;
  }

  @SchemaIgnore
  public boolean isDecrypted() {
    return decrypted;
  }

  public void setDecrypted(boolean decrypted) {
    this.decrypted = decrypted;
  }

  public SettingValue(String type) {
    this.type = type;
  }

  @SchemaIgnore
  public SettingVariableTypes getSettingType() {
    return SettingVariableTypes.valueOf(type);
  }

  public void setSettingType(SettingVariableTypes type) {}

  @JsonIgnore
  @SchemaIgnore
  public List<Field> getEncryptedFields() {
    return SettingValueHelper.getAllEncryptedFields(this);
  }

  public abstract String fetchResourceCategory();

  // Default Implementation
  public List<String> fetchRelevantEncryptedSecrets() {
    return SettingValueHelper.getAllEncryptedSecrets(this);
  }

  public boolean shouldDeleteArtifact(SettingValue prev) {
    return false;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public abstract static class Yaml extends BaseEntityYaml {
    private UsageRestrictions.Yaml usageRestrictions;

    public Yaml(String type, String harnessApiVersion, UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion);
      this.usageRestrictions = usageRestrictions;
    }
  }
}
