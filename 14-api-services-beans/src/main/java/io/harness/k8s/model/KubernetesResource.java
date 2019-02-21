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
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretEnvSource;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.fabric8.kubernetes.api.model.SecretVolumeSource;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.extensions.DaemonSet;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.StatefulSet;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.harness.exception.KubernetesYamlException;
import io.harness.k8s.manifest.ObjectYamlUtils;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;

@Data
@Builder
public class KubernetesResource {
  private KubernetesResourceId resourceId;
  private Object value;
  private String spec;

  private static KubernetesClient k8sClient = new DefaultKubernetesClient().inAnyNamespace();

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

  public KubernetesResource addLabelsInDeploymentSelector(Map<String, String> labels) {
    HasMetadata resource = k8sClient.load(IOUtils.toInputStream(this.spec, UTF_8)).get().get(0);
    Deployment deployment = (Deployment) resource;

    if (deployment.getSpec().getSelector() == null) {
      throw new KubernetesYamlException("Deployment spec does not have selector");
    }

    Map<String, String> matchLabels = deployment.getSpec().getSelector().getMatchLabels();
    if (matchLabels == null) {
      matchLabels = new HashMap<>();
    }

    matchLabels.putAll(labels);

    deployment.getSpec().getSelector().setMatchLabels(matchLabels);

    try {
      this.spec = KubernetesHelper.toYaml(resource);
      this.value = readYaml(this.spec).get(0);
    } catch (IOException e) {
      // do nothing
      noop();
    }

    return this;
  }

  public KubernetesResource setReplicaCount(Integer replicas) {
    HasMetadata resource = k8sClient.load(IOUtils.toInputStream(this.spec, UTF_8)).get().get(0);
    Deployment deployment = (Deployment) resource;

    deployment.getSpec().setReplicas(replicas);

    try {
      this.spec = KubernetesHelper.toYaml(resource);
      this.value = readYaml(this.spec).get(0);
    } catch (IOException e) {
      // do nothing
      noop();
    }

    return this;
  }

  public Integer getReplicaCount() {
    HasMetadata resource = k8sClient.load(IOUtils.toInputStream(this.spec, UTF_8)).get().get(0);
    Deployment deployment = (Deployment) resource;
    return deployment.getSpec().getReplicas();
  }

  public boolean isPrimaryService() {
    if (StringUtils.equals(Kind.Service.name(), this.getResourceId().getKind())) {
      String isPrimary = (String) this.getField("metadata.annotations." + encodeDot(HarnessAnnotations.primaryService));
      if (StringUtils.equalsIgnoreCase(isPrimary, "true")) {
        return true;
      }
    }
    return false;
  }

  public boolean isStageService() {
    if (StringUtils.equals(Kind.Service.name(), this.getResourceId().getKind())) {
      String isStage = (String) this.getField("metadata.annotations." + encodeDot(HarnessAnnotations.stageService));
      if (StringUtils.equalsIgnoreCase(isStage, "true")) {
        return true;
      }
    }
    return false;
  }

  public KubernetesResource addColorSelectorInService(String color) {
    HasMetadata resource = k8sClient.load(IOUtils.toInputStream(this.spec, UTF_8)).get().get(0);
    Service service = (Service) resource;

    Map<String, String> selectors = service.getSpec().getSelector();
    if (selectors == null) {
      selectors = new HashMap<>();
    }

    selectors.put(HarnessLabels.color, String.valueOf(color));

    service.getSpec().setSelector(selectors);

    try {
      this.spec = KubernetesHelper.toYaml(resource);
      this.value = readYaml(this.spec).get(0);
    } catch (IOException e) {
      // do nothing
      noop();
    }

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

  public KubernetesResource appendSuffixInName(String suffix) {
    UnaryOperator<Object> addSuffix = t -> t + suffix;
    this.transformName(addSuffix);
    return this;
  }

  public KubernetesResource addLabelsInPodSpec(Map<String, String> labels) {
    HasMetadata resource = k8sClient.load(IOUtils.toInputStream(this.spec, UTF_8)).get().get(0);

    PodTemplateSpec podTemplateSpec = getPodTemplateSpec(resource);

    Map<String, String> podLabels = podTemplateSpec.getMetadata().getLabels();
    if (podLabels == null) {
      podLabels = new HashMap<>();
    }

    podLabels.putAll(labels);

    podTemplateSpec.getMetadata().setLabels(podLabels);

    try {
      this.spec = KubernetesHelper.toYaml(resource);
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

  public static String redactSecretValues(String spec) {
    List<HasMetadata> output = k8sClient.load(IOUtils.toInputStream(spec, UTF_8)).get();
    if (!StringUtils.equals("Secret", output.get(0).getKind())) {
      return spec;
    }

    Secret secret = (Secret) output.get(0);
    final String redacted = "***";

    for (Entry e : secret.getData().entrySet()) {
      e.setValue(redacted);
    }

    for (Entry e : secret.getStringData().entrySet()) {
      e.setValue(redacted);
    }

    String result = "";

    try {
      result = KubernetesHelper.toYaml(secret);
    } catch (IOException e) {
      // do nothing
      noop();
    }
    return result;
  }

  private PodTemplateSpec getPodTemplateSpec(HasMetadata resource) {
    switch (resource.getKind()) {
      case "Deployment":
        return ((Deployment) resource).getSpec().getTemplate();
      case "DaemonSet":
        return ((DaemonSet) resource).getSpec().getTemplate();
      case "StatefulSet":
        return ((StatefulSet) resource).getSpec().getTemplate();
      case "Job":
        return ((Job) resource).getSpec().getTemplate();
      default:
        unhandled(resource.getKind());
    }
    return null;
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
    for (Container container : podSpec.getContainers()) {
      for (EnvVar envVar : container.getEnv()) {
        EnvVarSource envVarSource = envVar.getValueFrom();
        if (envVarSource != null) {
          SecretKeySelector secretKeyRef = envVarSource.getSecretKeyRef();
          if (secretKeyRef != null) {
            String name = secretKeyRef.getName();
            secretKeyRef.setName((String) transformer.apply(name));
          }
        }
      }

      for (EnvFromSource envFromSource : container.getEnvFrom()) {
        SecretEnvSource secretRef = envFromSource.getSecretRef();
        if (secretRef != null) {
          String name = secretRef.getName();
          secretRef.setName((String) transformer.apply(name));
        }
      }
    }

    for (LocalObjectReference imagePullSecret : podSpec.getImagePullSecrets()) {
      String name = imagePullSecret.getName();
      imagePullSecret.setName((String) transformer.apply(name));
    }

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
