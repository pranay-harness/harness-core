package io.harness.perpetualtask.k8s.cronjobs.client.impl;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.harness.perpetualtask.k8s.cronjobs.client.K8sCronJobClient;
import io.harness.perpetualtask.k8s.cronjobs.client.model.CronJob;
import io.harness.perpetualtask.k8s.cronjobs.client.model.CronJobList;
import io.harness.perpetualtask.k8s.cronjobs.client.model.DoneableCronJob;
import okhttp3.OkHttpClient;

public class DefaultK8sCronJobClient extends DefaultKubernetesClient implements K8sCronJobClient {
  public DefaultK8sCronJobClient(OkHttpClient httpClient, Config config) {
    super(httpClient, config);
  }

  @Override
  public MixedOperation<CronJob, CronJobList, DoneableCronJob, Resource<CronJob, DoneableCronJob>> cronJobs() {
    return new CronJobOperationsImpl(getHttpClient(), getConfiguration(), getNamespace());
  }
}
