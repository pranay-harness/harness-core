package software.wings.beans.ce;

import static software.wings.audit.ResourceType.CE_CONNECTOR;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;

import software.wings.settings.SettingValue;
import software.wings.settings.SettingVariableTypes;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

@JsonTypeName("CE_GCP")
@Data
@FieldNameConstants(innerTypeName = "CEGcpConfigKeys")
@EqualsAndHashCode(callSuper = false)
public class CEGcpConfig extends SettingValue {
  private String organizationSettingId;

  @Override
  public String fetchResourceCategory() {
    return CE_CONNECTOR.name();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return null;
  }

  public CEGcpConfig() {
    super(SettingVariableTypes.CE_GCP.name());
  }

  @Builder
  public CEGcpConfig(String organizationSettingId) {
    this();
    this.organizationSettingId = organizationSettingId;
  }
}
