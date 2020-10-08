package io.harness.cvng.core.activity.entities;

import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;

import com.google.common.base.Preconditions;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cvng.beans.ActivityDTO;
import io.harness.cvng.beans.ActivityType;
import io.harness.cvng.beans.KubernetesActivityDTO;
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
public class KubernetesActivity extends Activity {
  private String clusterName;
  private String activityDescription;

  @Override
  public ActivityType getType() {
    return ActivityType.INFRASTRUCTURE;
  }

  @Override
  public void fromDTO(ActivityDTO activityDTO) {
    Preconditions.checkState(activityDTO instanceof KubernetesActivityDTO);
    KubernetesActivityDTO kubernetesActivityDTO = (KubernetesActivityDTO) activityDTO;
    setClusterName(kubernetesActivityDTO.getClusterName());
    setActivityDescription(kubernetesActivityDTO.getActivityDescription());
    setType(ActivityType.INFRASTRUCTURE);
    addCommonFileds(activityDTO);
  }

  @Override
  public void validateActivityParams() {
    Preconditions.checkNotNull(clusterName, generateErrorMessageFromParam(KubernetesActivityKeys.clusterName));
  }
}
