package io.harness.perpetualtask.k8s.watch;

import static java.util.Objects.isNull;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Quantity;
import io.harness.perpetualtask.k8s.watch.Resource.Quantity.Builder;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

@UtilityClass
@Slf4j
public class K8sResourceUtils {
  String K8S_CPU_RESOURCE = "cpu";
  String K8S_MEMORY_RESOURCE = "memory";

  public static Resource getResource(io.fabric8.kubernetes.api.model.Container k8sContainer) {
    // get the resource for each container
    io.harness.perpetualtask.k8s.watch.Resource.Builder resourceBuilder =
        Resource.newBuilder()
            .putRequests(K8S_CPU_RESOURCE, getCpuRequest(k8sContainer))
            .putRequests(K8S_MEMORY_RESOURCE, getMemRequest(k8sContainer))
            .putLimits(K8S_CPU_RESOURCE, getCpuLimit(k8sContainer))
            .putLimits(K8S_MEMORY_RESOURCE, getMemLimit(k8sContainer));
    return resourceBuilder.build();
  }

  public static Resource.Quantity getCpuRequest(io.fabric8.kubernetes.api.model.Container k8sContainer) {
    Builder cpuRequestBuilder = Resource.Quantity.newBuilder();
    if (isNull(k8sContainer.getResources())) {
      logger.warn("This k8s container is missing resource.");
      return cpuRequestBuilder.build();
    }

    Map<String, io.fabric8.kubernetes.api.model.Quantity> resourceRequestsMap =
        k8sContainer.getResources().getRequests();
    if (resourceRequestsMap != null && resourceRequestsMap.get(K8S_CPU_RESOURCE) != null) {
      String cpuRequestAmount = resourceRequestsMap.get(K8S_CPU_RESOURCE).getAmount();
      if (!StringUtils.isBlank(cpuRequestAmount)) {
        cpuRequestBuilder.setAmount(cpuRequestAmount);
      }
      String cpuRequestFormat = resourceRequestsMap.get(K8S_CPU_RESOURCE).getFormat();
      if (!StringUtils.isBlank(cpuRequestFormat)) {
        cpuRequestBuilder.setUnit(cpuRequestFormat); // TODO: change this
      }
    }
    return cpuRequestBuilder.build();
  }

  public static Resource.Quantity getMemRequest(io.fabric8.kubernetes.api.model.Container k8sContainer) {
    Builder memRequestBuilder = Resource.Quantity.newBuilder();
    if (isNull(k8sContainer.getResources())) {
      logger.warn("This k8s container is missing resource.");
      return memRequestBuilder.build();
    }

    Map<String, io.fabric8.kubernetes.api.model.Quantity> resourceRequestsMap =
        k8sContainer.getResources().getRequests();
    if (resourceRequestsMap != null && resourceRequestsMap.get(K8S_MEMORY_RESOURCE) != null) {
      String memRequestAmount = resourceRequestsMap.get(K8S_MEMORY_RESOURCE).getAmount();
      if (!StringUtils.isBlank(memRequestAmount)) {
        memRequestBuilder.setAmount(memRequestAmount);
      }
      String memRequestFormat = resourceRequestsMap.get(K8S_MEMORY_RESOURCE).getFormat();
      if (!StringUtils.isBlank(memRequestFormat)) {
        memRequestBuilder.setUnit(memRequestFormat); // TODO: change this
      }
    }
    return memRequestBuilder.build();
  }

  public static Resource.Quantity getCpuLimit(io.fabric8.kubernetes.api.model.Container k8sContainer) {
    Builder cpuLimitBuilder = Resource.Quantity.newBuilder();
    if (isNull(k8sContainer.getResources())) {
      logger.warn("This k8s container is missing resource.");
      return cpuLimitBuilder.build();
    }

    Map<String, io.fabric8.kubernetes.api.model.Quantity> resourceLimitsMap = k8sContainer.getResources().getLimits();
    if (resourceLimitsMap != null && resourceLimitsMap.get(K8S_CPU_RESOURCE) != null) {
      String cpuLimitAmount = resourceLimitsMap.get(K8S_CPU_RESOURCE).getAmount();
      if (!StringUtils.isBlank(cpuLimitAmount)) {
        cpuLimitBuilder.setAmount(cpuLimitAmount);
      }
      String cpuLimitFormat = resourceLimitsMap.get(K8S_CPU_RESOURCE).getFormat();
      if (!StringUtils.isBlank(cpuLimitFormat)) {
        cpuLimitBuilder.setUnit(cpuLimitFormat); // TODO: change this
      }
    }
    return cpuLimitBuilder.build();
  }

  public static Resource.Quantity getMemLimit(io.fabric8.kubernetes.api.model.Container k8sContainer) {
    Builder memLimitBuilder = Resource.Quantity.newBuilder();
    if (isNull(k8sContainer.getResources())) {
      logger.warn("This k8s container is missing resource.");
      return memLimitBuilder.build();
    }

    Map<String, io.fabric8.kubernetes.api.model.Quantity> resourceLimitsMap = k8sContainer.getResources().getLimits();
    if (resourceLimitsMap != null && resourceLimitsMap.get(K8S_MEMORY_RESOURCE) != null) {
      String memLimitAmount = resourceLimitsMap.get(K8S_MEMORY_RESOURCE).getAmount();
      if (!StringUtils.isBlank(memLimitAmount)) {
        memLimitBuilder.setAmount(memLimitAmount);
      }
      String memLimitFormat = resourceLimitsMap.get(K8S_MEMORY_RESOURCE).getFormat();
      if (!StringUtils.isBlank(memLimitFormat)) {
        memLimitBuilder.setUnit(memLimitFormat); // TODO: change this
      }
    }
    return memLimitBuilder.build();
  }

  // for now, only resource request is taken into consideration
  public static Resource getTotalResourceRequest(List<Container> k8sContainerList) {
    float totalCpuRequestAmount = 0; // in cpu unit
    String totalCpuRequestUnit = "";
    float totalMemoryRequestAmount = 0;
    String totalMemoryRequestUnit = "M"; // TODO: convert all the amount in MB
    for (io.fabric8.kubernetes.api.model.Container k8sContainer : k8sContainerList) {
      Map<String, io.fabric8.kubernetes.api.model.Quantity> resourceRequestsMap =
          k8sContainer.getResources().getRequests();
      if (isNull(resourceRequestsMap)) {
        logger.warn("The resource request for the container with name={} is missing.", k8sContainer.getName());
      } else {
        totalCpuRequestAmount +=
            K8sResourceUtils.getCpuResourceRequest(k8sContainer.getName(), k8sContainer.getResources().getRequests());
        totalMemoryRequestAmount +=
            K8sResourceUtils.getMemResourceRequest(k8sContainer.getName(), k8sContainer.getResources().getRequests());
      }
    }
    return getResource(totalCpuRequestAmount, totalCpuRequestUnit, totalMemoryRequestAmount, totalMemoryRequestUnit);
  }

  public static Resource getResource(String resourceContainer, Map<String, Quantity> allocatableResource) {
    float totalCpuRequestAmount = getCpuResourceRequest(resourceContainer, allocatableResource);
    String totalCpuRequestUnit = "";
    float totalMemoryRequestAmount = getMemResourceRequest(resourceContainer, allocatableResource);
    String totalMemoryRequestUnit = "M"; // TODO: check this.
    return getResource(totalCpuRequestAmount, totalCpuRequestUnit, totalMemoryRequestAmount, totalMemoryRequestUnit);
  }

  public static Resource getResource(float totalCpuRequestAmount, String totalCpuRequestUnit,
      float totalMemoryRequestAmount, String totalMemoryRequestUnit) {
    io.harness.perpetualtask.k8s.watch.Resource.Builder totalResourceBuilder = Resource.newBuilder();

    Builder cpuRequestBuilder = Resource.Quantity.newBuilder();
    cpuRequestBuilder.setAmount(Float.toString(totalCpuRequestAmount));
    cpuRequestBuilder.setUnit(totalCpuRequestUnit);

    Builder memRequestBuilder = Resource.Quantity.newBuilder();
    memRequestBuilder.setAmount(Float.toString(totalMemoryRequestAmount));
    memRequestBuilder.setUnit(totalMemoryRequestUnit);

    totalResourceBuilder.putRequests(K8S_CPU_RESOURCE, cpuRequestBuilder.build())
        .putRequests(K8S_MEMORY_RESOURCE, memRequestBuilder.build());
    return totalResourceBuilder.build();
  }

  public static float getCpuResourceRequest(String resourceContainer, Map<String, Quantity> resourceRequestsMap) {
    float cpuRequestValue = 0;
    String cpuRequestUnit = "";
    Quantity cpuResourceRequest = resourceRequestsMap.get(K8S_CPU_RESOURCE);
    if (isNull(cpuResourceRequest)) {
      logger.error("The cpu resource request for the container with name={} is missing.", resourceContainer);
    } else {
      String cpuRequestAmount = cpuResourceRequest.getAmount();
      if (!StringUtils.isBlank(cpuRequestAmount)) {
        // aggregate the requested cpu resource
        cpuRequestValue = Float.valueOf(getResourceValue(cpuRequestAmount));
        cpuRequestUnit = getResourceUnit(cpuRequestAmount); // TODO: cpu unit could be "" or "m"
        logger.trace("{}", cpuRequestUnit);
      }

      String cpuRequestFormat = cpuResourceRequest.getFormat();
      if (!StringUtils.isBlank(cpuRequestFormat)) {
        // the format for cpu is the same
        logger.warn("The requested cpu resource has format of {}", cpuRequestFormat);
      }
    }

    return cpuRequestValue;
  }

  public static float getMemResourceRequest(String resourceContainer, Map<String, Quantity> resourceRequestsMap) {
    float memRequestValue = 0;
    String memRequestUnit = "";
    Quantity memResourceRequest = resourceRequestsMap.get(K8S_MEMORY_RESOURCE);
    if (isNull(memResourceRequest)) {
      logger.error("The memory resource request for the container with name={} is missing.", resourceContainer);
    } else {
      String memRequestAmount = memResourceRequest.getAmount();
      if (!StringUtils.isBlank(memRequestAmount)) {
        memRequestValue = Float.valueOf(getResourceValue(memRequestAmount));
        memRequestUnit = getResourceUnit(memRequestAmount);
        logger.trace("{}", memRequestUnit);
      }
      String memRequestFormat = memResourceRequest.getFormat();
      if (!StringUtils.isBlank(memRequestFormat)) {
        // TODO: check the format of the memory
        logger.warn("The requested memory resource has format of {}", memRequestFormat);
      }
    }
    return memRequestValue;
  }

  private static String getResourceValue(String s) {
    // replace all the letters with empty string
    return s.replaceAll("[a-zA-Z]+", "");
  }

  private static String getResourceUnit(String s) {
    return s.replaceAll("\\d+(\\.\\d+)?", "");
  }
}
