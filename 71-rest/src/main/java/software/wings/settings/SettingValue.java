package software.wings.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.wings.service.impl.SettingServiceHelper;
import software.wings.yaml.BaseEntityYaml;

import java.lang.reflect.Field;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = As.EXISTING_PROPERTY)
public abstract class SettingValue implements ExecutionCapabilityDemander {
  @Getter @Setter String type;
  @JsonIgnore @SchemaIgnore private transient boolean decrypted;

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
    return SettingServiceHelper.getAllEncryptedFields(this);
  }

  public abstract String fetchResourceCategory();

  // Default Implementation
  public List<String> fetchRelevantEncryptedSecrets() {
    return SettingServiceHelper.getAllEncryptedSecrets(this);
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
