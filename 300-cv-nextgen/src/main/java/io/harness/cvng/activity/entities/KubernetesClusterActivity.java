package io.harness.cvng.activity.entities;

import static io.harness.cvng.beans.activity.ActivityType.KUBERNETES;

import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.beans.change.KubernetesChangeEventMetadata.Action;
import io.harness.cvng.beans.change.KubernetesChangeEventMetadata.KubernetesResourceType;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.VerificationJobInstanceBuilder;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.query.UpdateOperations;

@FieldNameConstants(innerTypeName = "KubernetesClusterActivityKeys")
@Data
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonTypeName("KUBERNETES")
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class KubernetesClusterActivity extends Activity {
  String oldYaml;
  String newYaml;
  String workload;
  String namespace;
  String kind;
  KubernetesResourceType resourceType;
  Action action;
  String reason;
  String message;
  String resourceVersion;
  @Override
  public ActivityType getType() {
    return KUBERNETES;
  }

  @Override
  public void fromDTO(ActivityDTO activityDTO) {
    throw new UnsupportedOperationException("KubernetesClusterActivity can be transformed only from ChangeEventDTO");
  }

  @Override
  public void fillInVerificationJobInstanceDetails(VerificationJobInstanceBuilder verificationJobInstanceBuilder) {
    throw new UnsupportedOperationException(
        "We are currently not supporting analysis for KubernetesClusterActivity in ChI");
  }

  @Override
  public void validateActivityParams() {}

  @Override
  public boolean deduplicateEvents() {
    return false;
  }

  public static class KubernetesClusterActivityUpdatableEntity
      extends ActivityUpdatableEntity<KubernetesClusterActivity, KubernetesClusterActivity> {
    @Override
    public Class getEntityClass() {
      return KubernetesClusterActivity.class;
    }

    @Override
    public void setUpdateOperations(
        UpdateOperations<KubernetesClusterActivity> updateOperations, KubernetesClusterActivity activity) {
      setCommonUpdateOperations(updateOperations, activity);
      updateOperations.set(KubernetesClusterActivityKeys.oldYaml, activity.getOldYaml())
          .set(KubernetesClusterActivityKeys.newYaml, activity.getNewYaml())
          .set(KubernetesClusterActivityKeys.namespace, activity.getNamespace())
          .set(KubernetesClusterActivityKeys.workload, activity.getWorkload())
          .set(KubernetesClusterActivityKeys.kind, activity.getKind())
          .set(KubernetesClusterActivityKeys.resourceType, activity.getResourceType())
          .set(KubernetesClusterActivityKeys.action, activity.getAction())
          .set(KubernetesClusterActivityKeys.reason, activity.getReason())
          .set(KubernetesClusterActivityKeys.resourceVersion, activity.getResourceVersion())
          .set(KubernetesClusterActivityKeys.message, activity.getMessage());
    }
  }
}
