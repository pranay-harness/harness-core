/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.activity.entities;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
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
@OwnedBy(HarnessTeam.CV)
public class KubernetesActivitySource extends ActivitySource {
  public static final String SERVICE_IDENTIFIER_KEY =
      KubernetesActivitySourceKeys.activitySourceConfigs + "." + KubernetesActivitySourceConfigKeys.serviceIdentifier;

  @NotNull String connectorIdentifier;
  @NotNull @NotEmpty Set<KubernetesActivitySourceConfig> activitySourceConfigs;

  public ActivitySourceDTO toDTO() {
    return fillCommon(KubernetesActivitySourceDTO.builder())
        .uuid(getUuid())
        .connectorIdentifier(connectorIdentifier)
        .activitySourceConfigs(activitySourceConfigs)
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

  public static class KubernetesActivitySourceUpdatableEntity<T extends KubernetesActivitySource, D
                                                                  extends KubernetesActivitySourceDTO>
      extends ActivitySourceUpdatableEntity<T, D> {
    @Override
    public void setUpdateOperations(UpdateOperations<T> updateOperations, D dto) {
      setCommonOperations(updateOperations, dto);
      updateOperations.set(KubernetesActivitySourceKeys.connectorIdentifier, dto.getConnectorIdentifier())
          .set(KubernetesActivitySourceKeys.activitySourceConfigs, dto.getActivitySourceConfigs());
    }
  }
}
