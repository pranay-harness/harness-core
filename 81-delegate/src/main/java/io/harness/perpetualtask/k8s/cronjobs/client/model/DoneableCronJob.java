package io.harness.perpetualtask.k8s.cronjobs.client.model;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class DoneableCronJob extends CustomResourceDoneable<CronJob> {
  public DoneableCronJob(CronJob resource, Function<CronJob, CronJob> function) {
    super(resource, function);
  }
}
