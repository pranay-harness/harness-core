package io.harness.k8s.model;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.govern.Switch.noop;
import static io.harness.govern.Switch.unhandled;
import static io.harness.k8s.manifest.ObjectYamlUtils.encodeDot;
import static io.harness.k8s.manifest.ObjectYamlUtils.readYaml;
import static io.harness.validation.Validator.notNullCheck;

import io.harness.exception.KubernetesYamlException;
import io.harness.k8s.manifest.ObjectYamlUtils;
import io.harness.k8s.manifest.ResourceUtils;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapEnvSource;
import io.kubernetes.client.openapi.models.V1ConfigMapKeySelector;
import io.kubernetes.client.openapi.models.V1ConfigMapProjection;
import io.kubernetes.client.openapi.models.V1ConfigMapVolumeSource;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1EnvFromSource;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1EnvVarSource;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1LocalObjectReference;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretEnvSource;
import io.kubernetes.client.openapi.models.V1SecretKeySelector;
import io.kubernetes.client.openapi.models.V1SecretProjection;
import io.kubernetes.client.openapi.models.V1SecretVolumeSource;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.openapi.models.V1VolumeProjection;
import io.kubernetes.client.util.Yaml;
import lombok.Builder;
import lombok.Data;
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
  private static final String DEPLOYMENT = "Deployment";
  private static final String CONFIGMAP = "ConfigMap";
  private static final String SECRET = "Secret";
  private static final String SERVICE = "Service";
  private static final String DAEMONSET = "DaemonSet";
  private static final String STATEFULSET = "StatefulSet";
  private static final String JOB = "Job";
  private static final String POD = "Pod";

  private static final String MISSING_DEPLOYMENT_SPEC_MSG = "Deployment does not have spec";

  private KubernetesResourceId resourceId;
  private Object value;
  private String spec;

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
    Object k8sResource = getK8sResource();
    V1Deployment v1Deployment = (V1Deployment) k8sResource;
    notNullCheck(MISSING_DEPLOYMENT_SPEC_MSG, v1Deployment.getSpec());
    if (v1Deployment.getSpec().getSelector() == null) {
      throw new KubernetesYamlException("Deployment spec does not have selector");
    }

    Map<String, String> matchLabels = v1Deployment.getSpec().getSelector().getMatchLabels();
    if (matchLabels == null) {
      matchLabels = new HashMap<>();
    }

    matchLabels.putAll(labels);
    v1Deployment.getSpec().getSelector().setMatchLabels(matchLabels);

    try {
      this.spec = Yaml.dump(k8sResource);
      this.value = readYaml(this.spec).get(0);
    } catch (IOException e) {
      // do nothing
      noop();
    }

    return this;
  }

  public KubernetesResource setReplicaCount(Integer replicas) {
    Object k8sResource = getK8sResource();
    V1Deployment v1Deployment = (V1Deployment) k8sResource;
    notNullCheck(MISSING_DEPLOYMENT_SPEC_MSG, v1Deployment.getSpec());
    v1Deployment.getSpec().setReplicas(replicas);

    try {
      this.spec = Yaml.dump(k8sResource);
      this.value = readYaml(this.spec).get(0);
    } catch (IOException e) {
      // do nothing
      noop();
    }

    return this;
  }

  public Integer getReplicaCount() {
    Object k8sResource = getK8sResource();
    V1Deployment v1Deployment = (V1Deployment) k8sResource;
    notNullCheck(MISSING_DEPLOYMENT_SPEC_MSG, v1Deployment.getSpec());
    return v1Deployment.getSpec().getReplicas();
  }

  public boolean isService() {
    return StringUtils.equals(Kind.Service.name(), this.getResourceId().getKind());
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

  public boolean isLoadBalancerService() {
    if (!StringUtils.equals(Kind.Service.name(), this.getResourceId().getKind())) {
      return false;
    }

    Object k8sResource = getK8sResource();
    V1Service v1Service = (V1Service) k8sResource;
    notNullCheck("Service does not have spec", v1Service.getSpec());
    return StringUtils.equals(v1Service.getSpec().getType(), "LoadBalancer");
  }

  public KubernetesResource addColorSelectorInService(String color) {
    Object k8sResource = getK8sResource();
    V1Service v1Service = (V1Service) k8sResource;

    notNullCheck("Service does not have spec", v1Service.getSpec());
    Map<String, String> selectors = v1Service.getSpec().getSelector();
    if (selectors == null) {
      selectors = new HashMap<>();
    }

    selectors.put(HarnessLabels.color, String.valueOf(color));
    v1Service.getSpec().setSelector(selectors);

    try {
      this.spec = Yaml.dump(k8sResource);
      this.value = readYaml(this.spec).get(0);
    } catch (IOException e) {
      // do nothing
      noop();
    }

    return this;
  }

  public KubernetesResource transformName(UnaryOperator<Object> transformer) {
    Object k8sResource = getK8sResource();
    updateName(k8sResource, transformer);
    try {
      this.spec = Yaml.dump(k8sResource);
      this.value = readYaml(this.spec).get(0);
    } catch (IOException e) {
      // do nothing
      noop();
    }
    return this;
  }

  private void updateName(Object k8sResource, UnaryOperator<Object> transformer) {
    String newName;

    switch (this.resourceId.getKind()) {
      case DEPLOYMENT:
        V1Deployment v1Deployment = (V1Deployment) k8sResource;
        notNullCheck("Deployment does not have metadata", v1Deployment.getMetadata());
        newName = (String) transformer.apply(v1Deployment.getMetadata().getName());
        v1Deployment.getMetadata().setName(newName);
        this.resourceId.setName(newName);
        break;

      case CONFIGMAP:
        V1ConfigMap v1ConfigMap = (V1ConfigMap) k8sResource;
        notNullCheck("ConfigMap does not have metadata", v1ConfigMap.getMetadata());
        newName = (String) transformer.apply(v1ConfigMap.getMetadata().getName());
        v1ConfigMap.getMetadata().setName(newName);
        this.resourceId.setName(newName);
        break;

      case SECRET:
        V1Secret v1Secret = (V1Secret) k8sResource;
        notNullCheck("Secret does not have metadata", v1Secret.getMetadata());
        newName = (String) transformer.apply(v1Secret.getMetadata().getName());
        v1Secret.getMetadata().setName(newName);
        this.resourceId.setName(newName);
        break;

      case SERVICE:
        V1Service v1Service = (V1Service) k8sResource;
        notNullCheck("Service does not have metadata", v1Service.getMetadata());
        newName = (String) transformer.apply(v1Service.getMetadata().getName());
        v1Service.getMetadata().setName(newName);
        this.resourceId.setName(newName);
        break;

      default:
        unhandled(this.resourceId.getKind());
    }
  }

  public KubernetesResource appendSuffixInName(String suffix) {
    UnaryOperator<Object> addSuffix = t -> t + suffix;
    this.transformName(addSuffix);
    return this;
  }

  public KubernetesResource addLabelsInPodSpec(Map<String, String> labels) {
    Object k8sResource = getK8sResource();
    V1PodTemplateSpec v1PodTemplateSpec = getV1PodTemplateSpec(k8sResource);
    if (v1PodTemplateSpec == null) {
      return this;
    }

    if (v1PodTemplateSpec.getMetadata() == null) {
      v1PodTemplateSpec.setMetadata(new V1ObjectMeta());
    }

    notNullCheck("PodTemplateSpec does not have metadata", v1PodTemplateSpec.getMetadata());
    Map<String, String> podLabels = v1PodTemplateSpec.getMetadata().getLabels();
    if (podLabels == null) {
      podLabels = new HashMap<>();
    }

    podLabels.putAll(labels);

    v1PodTemplateSpec.getMetadata().setLabels(podLabels);

    try {
      this.spec = Yaml.dump(k8sResource);
      this.value = readYaml(this.spec).get(0);
    } catch (IOException e) {
      // do nothing
      noop();
    }
    return this;
  }

  public KubernetesResource transformConfigMapAndSecretRef(
      UnaryOperator<Object> configMapRefTransformer, UnaryOperator<Object> secretRefTransformer) {
    Object k8sResource = getK8sResource();
    V1PodSpec v1PodSpec = getV1PodSpec(k8sResource);
    if (v1PodSpec == null) {
      return this;
    }

    updateConfigMapRef(v1PodSpec, configMapRefTransformer);
    updateSecretRef(v1PodSpec, secretRefTransformer);

    try {
      this.spec = Yaml.dump(k8sResource);
      this.value = readYaml(this.spec).get(0);
    } catch (IOException e) {
      // do nothing
      noop();
    }
    return this;
  }

  public static String redactSecretValues(String spec) {
    String result = "Error in redactSecretValues. skipped.\n";

    try {
      V1Secret v1Secret = Yaml.loadAs(spec, V1Secret.class);

      final String redacted = "***";
      if (isNotEmpty(v1Secret.getData())) {
        for (Entry e : v1Secret.getData().entrySet()) {
          e.setValue(redacted);
        }
      }

      if (isNotEmpty(v1Secret.getStringData())) {
        for (Entry e : v1Secret.getStringData().entrySet()) {
          e.setValue(redacted);
        }
      }

      result = Yaml.dump(v1Secret);
    } catch (Exception e) {
      // do nothing
      noop();
    }
    return result;
  }

  private V1PodTemplateSpec getV1PodTemplateSpec(Object resource) {
    switch (this.resourceId.getKind()) {
      case DEPLOYMENT:
        notNullCheck(MISSING_DEPLOYMENT_SPEC_MSG, ((V1Deployment) resource).getSpec());
        return ((V1Deployment) resource).getSpec().getTemplate();
      case DAEMONSET:
        notNullCheck("DaemonSet does not have spec", ((V1DaemonSet) resource).getSpec());
        return ((V1DaemonSet) resource).getSpec().getTemplate();
      case STATEFULSET:
        notNullCheck("StatefulSet does not have spec", ((V1StatefulSet) resource).getSpec());
        return ((V1StatefulSet) resource).getSpec().getTemplate();
      case JOB:
        notNullCheck("Job does not have spec", ((V1Job) resource).getSpec());
        return ((V1Job) resource).getSpec().getTemplate();
      default:
        unhandled(this.resourceId.getKind());
    }

    return null;
  }

  private V1PodSpec getV1PodSpec(Object resource) {
    switch (this.resourceId.getKind()) {
      case DEPLOYMENT:
        notNullCheck(MISSING_DEPLOYMENT_SPEC_MSG, ((V1Deployment) resource).getSpec());
        return ((V1Deployment) resource).getSpec().getTemplate().getSpec();
      case DAEMONSET:
        notNullCheck("DaemonSet does not have spec", ((V1DaemonSet) resource).getSpec());
        return ((V1DaemonSet) resource).getSpec().getTemplate().getSpec();
      case STATEFULSET:
        notNullCheck("StatefulSet does not have spec", ((V1StatefulSet) resource).getSpec());
        return ((V1StatefulSet) resource).getSpec().getTemplate().getSpec();
      case JOB:
        notNullCheck("Job does not have spec", ((V1Job) resource).getSpec());
        return ((V1Job) resource).getSpec().getTemplate().getSpec();
      case POD:
        notNullCheck("Pod does not have spec", ((V1Pod) resource).getSpec());
        return ((V1Pod) resource).getSpec();
      default:
        unhandled(this.resourceId.getKind());
    }

    return null;
  }

  private Object getK8sResource() {
    switch (this.resourceId.getKind()) {
      case DEPLOYMENT:
        return Yaml.loadAs(this.spec, V1Deployment.class);
      case DAEMONSET:
        return Yaml.loadAs(this.spec, V1DaemonSet.class);
      case STATEFULSET:
        return Yaml.loadAs(this.spec, V1StatefulSet.class);
      case JOB:
        return Yaml.loadAs(this.spec, V1Job.class);
      case SERVICE:
        return Yaml.loadAs(this.spec, V1Service.class);
      case SECRET:
        return Yaml.loadAs(this.spec, V1Secret.class);
      case CONFIGMAP:
        return Yaml.loadAs(this.spec, V1ConfigMap.class);
      case POD:
        return Yaml.loadAs(this.spec, V1Pod.class);
      default:
        unhandled(this.resourceId.getKind());
        throw new KubernetesYamlException("Unhandled Kubernetes resource " + this.resourceId.getKind());
    }
  }

  private void updateConfigMapRef(V1PodSpec v1PodSpec, UnaryOperator<Object> transformer) {
    updateConfigMapRefInContainers(v1PodSpec.getContainers(), transformer);
    updateConfigMapRefInContainers(v1PodSpec.getInitContainers(), transformer);
    updateConfigMapRefInVolumes(v1PodSpec, transformer);
  }

  private void updateConfigMapRefInContainers(List<V1Container> containers, UnaryOperator<Object> transformer) {
    if (isNotEmpty(containers)) {
      for (V1Container v1Container : containers) {
        if (isNotEmpty(v1Container.getEnv())) {
          for (V1EnvVar v1EnvVar : v1Container.getEnv()) {
            V1EnvVarSource v1EnvVarSource = v1EnvVar.getValueFrom();
            if (v1EnvVarSource != null) {
              V1ConfigMapKeySelector v1ConfigMapKeyRef = v1EnvVarSource.getConfigMapKeyRef();
              if (v1ConfigMapKeyRef != null) {
                String name = v1ConfigMapKeyRef.getName();
                v1ConfigMapKeyRef.setName((String) transformer.apply(name));
              }
            }
          }
        }

        if (isNotEmpty(v1Container.getEnvFrom())) {
          for (V1EnvFromSource v1EnvFromSource : v1Container.getEnvFrom()) {
            V1ConfigMapEnvSource v1ConfigMapRef = v1EnvFromSource.getConfigMapRef();
            if (v1ConfigMapRef != null) {
              String name = v1ConfigMapRef.getName();
              v1ConfigMapRef.setName((String) transformer.apply(name));
            }
          }
        }
      }
    }
  }

  private void updateConfigMapRefInVolumes(V1PodSpec v1PodSpec, UnaryOperator<Object> transformer) {
    if (isNotEmpty(v1PodSpec.getVolumes())) {
      for (V1Volume v1Volume : v1PodSpec.getVolumes()) {
        V1ConfigMapVolumeSource v1ConfigMap = v1Volume.getConfigMap();
        if (v1ConfigMap != null) {
          String name = v1ConfigMap.getName();
          v1ConfigMap.setName((String) transformer.apply(name));
        }

        if (v1Volume.getProjected() != null && v1Volume.getProjected().getSources() != null) {
          for (V1VolumeProjection v1VolumeProjection : v1Volume.getProjected().getSources()) {
            V1ConfigMapProjection v1ConfigMapProjection = v1VolumeProjection.getConfigMap();
            if (v1ConfigMapProjection != null) {
              String name = v1ConfigMapProjection.getName();
              v1ConfigMapProjection.setName((String) transformer.apply(name));
            }
          }
        }
      }
    }
  }

  private void updateSecretRef(V1PodSpec v1PodSpec, UnaryOperator<Object> transformer) {
    updateSecretRefInContainers(v1PodSpec.getContainers(), transformer);
    updateSecretRefInContainers(v1PodSpec.getInitContainers(), transformer);
    updateSecretRefInImagePullSecrets(v1PodSpec, transformer);
    updateSecretRefInVolumes(v1PodSpec, transformer);
  }

  private void updateSecretRefInImagePullSecrets(V1PodSpec v1PodSpec, UnaryOperator<Object> transformer) {
    if (isNotEmpty(v1PodSpec.getImagePullSecrets())) {
      for (V1LocalObjectReference v1ImagePullSecret : v1PodSpec.getImagePullSecrets()) {
        String name = v1ImagePullSecret.getName();
        v1ImagePullSecret.setName((String) transformer.apply(name));
      }
    }
  }

  private void updateSecretRefInVolumes(V1PodSpec v1PodSpec, UnaryOperator<Object> transformer) {
    if (isNotEmpty(v1PodSpec.getVolumes())) {
      for (V1Volume v1Volume : v1PodSpec.getVolumes()) {
        V1SecretVolumeSource v1Secret = v1Volume.getSecret();
        if (v1Secret != null) {
          String name = v1Secret.getSecretName();
          v1Secret.setSecretName((String) transformer.apply(name));
        }

        if (v1Volume.getProjected() != null && v1Volume.getProjected().getSources() != null) {
          for (V1VolumeProjection v1VolumeProjection : v1Volume.getProjected().getSources()) {
            V1SecretProjection v1SecretProjection = v1VolumeProjection.getSecret();
            if (v1SecretProjection != null) {
              String name = v1SecretProjection.getName();
              v1SecretProjection.setName((String) transformer.apply(name));
            }
          }
        }
      }
    }
  }

  private void updateSecretRefInContainers(List<V1Container> containers, UnaryOperator<Object> transformer) {
    if (isNotEmpty(containers)) {
      for (V1Container v1Container : containers) {
        if (isNotEmpty(v1Container.getEnv())) {
          for (V1EnvVar v1EnvVar : v1Container.getEnv()) {
            V1EnvVarSource v1EnvVarSource = v1EnvVar.getValueFrom();
            if (v1EnvVarSource != null) {
              V1SecretKeySelector v1SecretKeyRef = v1EnvVarSource.getSecretKeyRef();
              if (v1SecretKeyRef != null) {
                String name = v1SecretKeyRef.getName();
                v1SecretKeyRef.setName((String) transformer.apply(name));
              }
            }
          }
        }

        if (isNotEmpty(v1Container.getEnvFrom())) {
          for (V1EnvFromSource v1EnvFromSource : v1Container.getEnvFrom()) {
            V1SecretEnvSource v1SecretRef = v1EnvFromSource.getSecretRef();
            if (v1SecretRef != null) {
              String name = v1SecretRef.getName();
              v1SecretRef.setName((String) transformer.apply(name));
            }
          }
        }
      }
    }
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

  public boolean isManaged() {
    String isManaged = (String) this.getField("metadata.annotations." + encodeDot(HarnessAnnotations.managed));
    return StringUtils.equalsIgnoreCase(isManaged, "true");
  }

  /* Issue https://github.com/kubernetes/kubernetes/pull/66165 was fixed in 1.11.2.
  The issue didn't allow update to stateful set which has empty/null fields in its spec. */
  public String getSpec() {
    if (!STATEFULSET.equals(resourceId.getKind())) {
      return spec;
    }

    try {
      return ResourceUtils.removeEmptyOrNullFields(Yaml.dump(Yaml.loadAs(this.spec, V1StatefulSet.class)));
    } catch (IOException e) {
      // Return original spec
      return spec;
    }
  }
}
