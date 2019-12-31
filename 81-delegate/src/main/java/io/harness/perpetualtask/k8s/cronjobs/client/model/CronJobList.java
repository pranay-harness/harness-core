package io.harness.perpetualtask.k8s.cronjobs.client.model;

import io.fabric8.kubernetes.client.CustomResourceList;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.List;

@NoArgsConstructor
public class CronJobList extends CustomResourceList<CronJob> {
  @Builder
  public CronJobList(@Singular List<CronJob> items) {
    this.setApiVersion("v1");
    this.setKind("List");
    this.setItems(items);
  }
}
