package io.harness.cvng.beans;

import io.harness.cvng.beans.activity.ActivitySourceDTO;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class K8ActivityDataCollectionInfo extends CVDataCollectionInfo {
  @Deprecated ActivitySourceDTO activitySourceDTO;
  String projectIdentifier;
  String orgIdentifier;
  String serviceIdentifier;
  String envIdentifier;
  String changeSourceIdentifier;
}
