package io.harness.k8s.model;

import static io.harness.govern.Switch.noop;
import static io.harness.govern.Switch.unhandled;
import static io.harness.k8s.manifest.ObjectYamlUtils.encodeDot;
import static io.harness.k8s.manifest.ObjectYamlUtils.readYaml;
import static java.nio.charset.StandardCharsets.UTF_8;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ConfigMapEnvSource;
import io.fabric8.kubernetes.api.model.ConfigMapKeySelector;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSource;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarSource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Job;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.SecretVolumeSource;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.extensions.DaemonSet;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.StatefulSet;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.harness.k8s.manifest.ObjectYamlUtils;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

@Data
@Builder
public class KubernetesResource {
  private KubernetesResourceId resourceId;
  private Object value;
  private String spec;

  private static KubernetesClient k8sClient = new DefaultKubernetesClient(new ConfigBuilder().build());

  private static final String dotMatch = "\\.";

  public Object getField(String key) {
    return ObjectYamlUtils.getField(this.getValue(), key);
  }

  public List<Object> getFields(String key) {
    return ObjectYamlUtils.getFields(this.getValue(), key);
  }

  public KubernetesResource setField(String key, Object newValue) {
    ObjectYamlUtils.setField(this.getValue(), key, newValue);
    return this;
  }

  public KubernetesResource transformName(UnaryOperator<Object> transformer) {
    List<HasMetadata> output = k8sClient.load(IOUtils.toInputStream(this.spec, UTF_8)).get();
    String newName = (String) transformer.apply(output.get(0).getMetadata().getName());
    output.get(0).getMetadata().setName(newName);
    this.resourceId.setName(newName);
    try {
      this.spec = KubernetesHelper.toYaml(output.get(0));
      this.value = readYaml(this.spec).get(0);
    } catch (IOException e) {
      // do nothing
      noop();
    }
    return this;
  }

  public KubernetesResource transformConfigMapAndSecretRef(
      UnaryOperator<Object> configMapRefTransformer, UnaryOperator<Object> secretRefTransformer) {
    HasMetadata resource = k8sClient.load(IOUtils.toInputStream(this.spec, UTF_8)).get().get(0);

    PodSpec podSpec = getPodSpec(resource);

    updateConfigMapRef(podSpec, configMapRefTransformer);
    updateSecretRef(podSpec, secretRefTransformer);

    try {
      this.spec = KubernetesHelper.toYaml(resource);
      this.value = readYaml(this.spec).get(0);
    } catch (IOException e) {
      // do nothing
      noop();
    }
    return this;
  }

  private PodSpec getPodSpec(HasMetadata resource) {
    switch (resource.getKind()) {
      case "Deployment":
        return ((Deployment) resource).getSpec().getTemplate().getSpec();
      case "DaemonSet":
        return ((DaemonSet) resource).getSpec().getTemplate().getSpec();
      case "StatefulSet":
        return ((StatefulSet) resource).getSpec().getTemplate().getSpec();
      case "Job":
        return ((Job) resource).getSpec().getTemplate().getSpec();
      case "Pod":
        return ((Pod) resource).getSpec();
      default:
        unhandled(resource.getKind());
    }
    return null;
  }

  private void updateConfigMapRef(PodSpec podSpec, UnaryOperator<Object> transformer) {
    for (Container container : podSpec.getContainers()) {
      for (EnvVar envVar : container.getEnv()) {
        EnvVarSource envVarSource = envVar.getValueFrom();
        if (envVarSource != null) {
          ConfigMapKeySelector configMapKeyRef = envVarSource.getConfigMapKeyRef();
          if (configMapKeyRef != null) {
            String name = configMapKeyRef.getName();
            configMapKeyRef.setName((String) transformer.apply(name));
          }
        }
      }

      for (EnvFromSource envFromSource : container.getEnvFrom()) {
        ConfigMapEnvSource configMapRef = envFromSource.getConfigMapRef();
        if (configMapRef != null) {
          String name = configMapRef.getName();
          configMapRef.setName((String) transformer.apply(name));
        }
      }
    }

    for (Volume volume : podSpec.getVolumes()) {
      ConfigMapVolumeSource configMap = volume.getConfigMap();
      if (configMap != null) {
        String name = configMap.getName();
        configMap.setName((String) transformer.apply(name));
      }
    }
  }

  private void updateSecretRef(PodSpec podSpec, UnaryOperator<Object> transformer) {
    for (Volume volume : podSpec.getVolumes()) {
      SecretVolumeSource secret = volume.getSecret();
      if (secret != null) {
        String name = secret.getSecretName();
        secret.setSecretName((String) transformer.apply(name));
      }
    }
  }

  public KubernetesResource transformField(String key, UnaryOperator<Object> transformer) {
    ObjectYamlUtils.transformField(this.getValue(), key, transformer);
    return this;
  }

  public KubernetesResource addAnnotations(Map newAnnotations) {
    Map annotations = (Map) this.getField("metadata.annotations");
    if (annotations == null) {
      annotations = new HashMap();
    }

    annotations.putAll(newAnnotations);
    return this.setField("metadata.annotations", annotations);
  }

  public KubernetesResource addLabels(Map newLabels) {
    Map labels = (Map) this.getField("metadata.labels");
    if (labels == null) {
      labels = new HashMap();
    }

    labels.putAll(newLabels);
    return this.setField("metadata.labels", labels);
  }

  public boolean isDirectApply() {
    String isDirectApply = (String) this.getField("metadata.annotations." + encodeDot(HarnessAnnotations.directApply));
    return StringUtils.equalsIgnoreCase(isDirectApply, "true");
  }
}
