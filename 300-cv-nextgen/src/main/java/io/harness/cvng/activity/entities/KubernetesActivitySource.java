package io.harness.cvng.activity.entities;

import io.harness.cvng.beans.activity.ActivitySourceDTO;
import io.harness.cvng.beans.activity.ActivitySourceType;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO.KubernetesActivitySourceConfig;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO.KubernetesActivitySourceConfig.KubernetesActivitySourceConfigKeys;

import com.google.common.base.Preconditions;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.UpdateOperations;

@Data
@NoArgsConstructor
@SuperBuilder
@FieldNameConstants(innerTypeName = "KubernetesActivitySourceKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class KubernetesActivitySource extends ActivitySource {
  public static final String SERVICE_IDENTIFIER_KEY =
      KubernetesActivitySourceKeys.activitySourceConfigs + "." + KubernetesActivitySourceConfigKeys.serviceIdentifier;

  @NotNull String connectorIdentifier;
  @NotNull @NotEmpty Set<KubernetesActivitySourceConfig> activitySourceConfigs;

  public ActivitySourceDTO toDTO() {
    return KubernetesActivitySourceDTO.builder()
        .uuid(getUuid())
        .identifier(getIdentifier())
        .name(getName())
        .connectorIdentifier(connectorIdentifier)
        .activitySourceConfigs(activitySourceConfigs)
        .createdAt(getCreatedAt())
        .lastUpdatedAt(getLastUpdatedAt())
        .build();
  }

  @Override
  protected void validateParams() {
    Preconditions.checkNotNull(connectorIdentifier);
    Preconditions.checkNotNull(activitySourceConfigs);
  }

  public static KubernetesActivitySource fromDTO(
      String accountId, String orgIdentifier, String projectIdentifier, KubernetesActivitySourceDTO activitySourceDTO) {
    return KubernetesActivitySource.builder()
        .uuid(activitySourceDTO.getUuid())
        .accountId(accountId)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .uuid(activitySourceDTO.getUuid())
        .identifier(activitySourceDTO.getIdentifier())
        .name(activitySourceDTO.getName())
        .connectorIdentifier(activitySourceDTO.getConnectorIdentifier())
        .activitySourceConfigs(activitySourceDTO.getActivitySourceConfigs())
        .type(ActivitySourceType.KUBERNETES)
        .build();
  }

  public static void setUpdateOperations(
      UpdateOperations<ActivitySource> updateOperations, KubernetesActivitySourceDTO activitySourceDTO) {
    updateOperations.set(KubernetesActivitySourceKeys.connectorIdentifier, activitySourceDTO.getConnectorIdentifier())
        .set(KubernetesActivitySourceKeys.activitySourceConfigs, activitySourceDTO.getActivitySourceConfigs());
  }
}
