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
}
