package io.harness.perpetualtask.k8s.cronjobs.client;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.harness.perpetualtask.k8s.cronjobs.client.model.CronJob;
import io.harness.perpetualtask.k8s.cronjobs.client.model.CronJobList;
import io.harness.perpetualtask.k8s.cronjobs.client.model.DoneableCronJob;

public interface K8sCronJobClient extends KubernetesClient {
  String CRON_API_GROUP = "batch";
  String CRON_API_VERSION = "v1beta1";

  MixedOperation<CronJob, CronJobList, DoneableCronJob, Resource<CronJob, DoneableCronJob>> cronJobs();
}
