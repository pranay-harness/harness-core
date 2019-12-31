package io.harness.perpetualtask.k8s.cronjobs.client.impl;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.dsl.internal.CustomResourceOperationsImpl;
import io.harness.perpetualtask.k8s.cronjobs.client.K8sCronJobClient;
import io.harness.perpetualtask.k8s.cronjobs.client.model.CronJob;
import io.harness.perpetualtask.k8s.cronjobs.client.model.CronJobList;
import io.harness.perpetualtask.k8s.cronjobs.client.model.DoneableCronJob;
import okhttp3.OkHttpClient;

class CronJobOperationsImpl extends CustomResourceOperationsImpl<CronJob, CronJobList, DoneableCronJob> {
  CronJobOperationsImpl(OkHttpClient client, Config config, String namespace) {
    super(client, config, K8sCronJobClient.CRON_API_GROUP, K8sCronJobClient.CRON_API_VERSION, "cronjobs", namespace,
        null, Boolean.TRUE, null, null, Boolean.FALSE, CronJob.class, CronJobList.class, DoneableCronJob.class);
  }
}
