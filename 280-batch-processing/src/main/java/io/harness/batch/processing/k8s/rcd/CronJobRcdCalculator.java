package io.harness.batch.processing.k8s.rcd;

import io.kubernetes.client.openapi.models.V1JobSpec;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1beta1CronJob;
import io.kubernetes.client.openapi.models.V1beta1CronJobSpec;
import io.kubernetes.client.openapi.models.V1beta1JobTemplateSpec;
import io.kubernetes.client.util.Yaml;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CronJobRcdCalculator implements ResourceClaimDiffCalculator {
  @Override
  public String getKind() {
    return "CronJob";
  }

  @Override
  public ResourceClaimDiff computeResourceClaimDiff(String oldYaml, String newYaml) {
    V1PodSpec oldPodSpec = Optional.ofNullable(Yaml.loadAs(oldYaml, V1beta1CronJob.class))
                               .map(V1beta1CronJob::getSpec)
                               .map(V1beta1CronJobSpec::getJobTemplate)
                               .map(V1beta1JobTemplateSpec::getSpec)
                               .map(V1JobSpec::getTemplate)
                               .map(V1PodTemplateSpec::getSpec)
                               .orElse(null);
    V1PodSpec newPodSpec = Optional.ofNullable(Yaml.loadAs(newYaml, V1beta1CronJob.class))
                               .map(V1beta1CronJob::getSpec)
                               .map(V1beta1CronJobSpec::getJobTemplate)
                               .map(V1beta1JobTemplateSpec::getSpec)
                               .map(V1JobSpec::getTemplate)
                               .map(V1PodTemplateSpec::getSpec)
                               .orElse(null);
    return ResourceClaimUtils.resourceClaimDiffForPod(oldPodSpec, newPodSpec);
  }
}
