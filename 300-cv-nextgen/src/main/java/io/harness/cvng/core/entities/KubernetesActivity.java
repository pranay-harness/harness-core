package io.harness.cvng.core.entities;

import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;

import com.google.common.base.Preconditions;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@JsonTypeName("INFRASTRUCTURE")
@Data
@FieldNameConstants(innerTypeName = "KubernetesActivityKeys")
@Builder
@AllArgsConstructor
public class KubernetesActivity extends Activity {
  private String clusterName;
  private String activityDescription;

  @Override
  public ActivityType getType() {
    return ActivityType.INFRASTRUCTURE;
  }

  @Override
  public void validateActivityParams() {
    Preconditions.checkNotNull(clusterName, generateErrorMessageFromParam(KubernetesActivityKeys.clusterName));
  }
}
