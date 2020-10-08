package io.harness.cvng.beans;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class K8ActivityDataCollectionInfo extends CVDataCollectionInfo {
  private KubernetesActivitySourceDTO activitySourceDTO;
}
