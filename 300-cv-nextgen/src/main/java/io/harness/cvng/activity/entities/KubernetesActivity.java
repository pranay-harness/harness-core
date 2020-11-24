package io.harness.cvng.activity.entities;

import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;

import io.harness.cvng.beans.ActivityDTO;
import io.harness.cvng.beans.ActivityType;
import io.harness.cvng.beans.KubernetesActivityDTO;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

@JsonTypeName("INFRASTRUCTURE")
@Data
@FieldNameConstants(innerTypeName = "KubernetesActivityKeys")
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class KubernetesActivity extends InfrastructureActivity {
  private String message;
  private String json;

  @Override
  public void fromDTO(ActivityDTO activityDTO) {
    Preconditions.checkState(activityDTO instanceof KubernetesActivityDTO);
    KubernetesActivityDTO kubernetesActivityDTO = (KubernetesActivityDTO) activityDTO;
    setMessage(kubernetesActivityDTO.getMessage());
    setJson(kubernetesActivityDTO.getJson());
    setType(ActivityType.INFRASTRUCTURE);
    addCommonFileds(activityDTO);
  }

  @Override
  public void validateActivityParams() {
    Preconditions.checkNotNull(message, generateErrorMessageFromParam(KubernetesActivityKeys.message));
    Preconditions.checkNotNull(json, generateErrorMessageFromParam(KubernetesActivityKeys.json));
  }
}
