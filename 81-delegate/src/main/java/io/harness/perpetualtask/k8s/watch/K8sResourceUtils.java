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

@Slf4j
@UtilityClass
public class K8sResourceUtils {
  public static Resource getResource(io.fabric8.kubernetes.api.model.Container k8sContainer) {
    // get the resource for each container
    Map<String, io.fabric8.kubernetes.api.model.Quantity> resourceLimitsMap = k8sContainer.getResources().getLimits();
    Map<String, io.fabric8.kubernetes.api.model.Quantity> resourceRequestsMap =
        k8sContainer.getResources().getRequests();

    io.harness.perpetualtask.k8s.watch.Resource.Builder resourceBuilder = Resource.newBuilder();
    if (resourceLimitsMap != null) {
      Builder cpuLimitBuilder = Resource.Quantity.newBuilder();
      if (resourceLimitsMap.get("cpu") != null) {
        String cpuLimitAmount = resourceLimitsMap.get("cpu").getAmount();
        if (!StringUtils.isBlank(cpuLimitAmount)) {
          cpuLimitBuilder.setAmount(cpuLimitAmount);
        }
        String cpuLimitFormat = resourceLimitsMap.get("cpu").getFormat();
        if (!StringUtils.isBlank(cpuLimitFormat)) {
          cpuLimitBuilder.setUnit(cpuLimitFormat); // TODO: change this
        }
      }

      Builder memLimitBuilder = Resource.Quantity.newBuilder();
      if (resourceLimitsMap.get("memory") != null) {
        String memLimitAmount = resourceLimitsMap.get("memory").getAmount();
        if (!StringUtils.isBlank(memLimitAmount)) {
          memLimitBuilder.setAmount(memLimitAmount);
        }
        String memLimitFormat = resourceLimitsMap.get("memory").getFormat();
        if (!StringUtils.isBlank(memLimitFormat)) {
          memLimitBuilder.setUnit(memLimitFormat); // TODO: change this
        }
      }
      resourceBuilder.putLimits("cpu", cpuLimitBuilder.build()).putLimits("memory", memLimitBuilder.build());
    }

    if (resourceRequestsMap != null) {
      Builder cpuRequestBuilder = Resource.Quantity.newBuilder();
      if (resourceRequestsMap.get("cpu") != null) {
        String cpuRequestAmount = resourceRequestsMap.get("cpu").getAmount();
        if (!StringUtils.isBlank(cpuRequestAmount)) {
          cpuRequestBuilder.setAmount(cpuRequestAmount);
        }
        String cpuRequestFormat = resourceRequestsMap.get("cpu").getFormat();
        if (!StringUtils.isBlank(cpuRequestFormat)) {
          cpuRequestBuilder.setUnit(cpuRequestFormat); // TODO: change this
        }
      }

      Builder memRequestBuilder = Resource.Quantity.newBuilder();
      if (resourceRequestsMap.get("memory") != null) {
        String memRequestAmount = resourceRequestsMap.get("memory").getAmount();
        if (!StringUtils.isBlank(memRequestAmount)) {
          memRequestBuilder.setAmount(memRequestAmount);
        }
        String memRequestFormat = resourceRequestsMap.get("memory").getFormat();
        if (!StringUtils.isBlank(memRequestFormat)) {
          memRequestBuilder.setUnit(memRequestFormat); // TODO: change this
        }
      }
      resourceBuilder.putRequests("cpu", cpuRequestBuilder.build()).putRequests("memory", memRequestBuilder.build());
    }
    return resourceBuilder.build();
  }

  // for now, only resource request is taken into consideration
  public static Resource getTotalResourceRequest(List<Container> k8sContainerList) {
    io.harness.perpetualtask.k8s.watch.Resource.Builder totalResourceBuilder = Resource.newBuilder();
    float totalCpuRequestAmount = 0; // in cpu unit
    String totalCpuRequestUnit = "";
    float totalMemoryRequestAmount = 0;
    String totalMemoryRequestUnit = "M"; // TODO: check this.
    for (io.fabric8.kubernetes.api.model.Container k8sContainer : k8sContainerList) {
      Map<String, io.fabric8.kubernetes.api.model.Quantity> resourceRequestsMap =
          k8sContainer.getResources().getRequests();
      if (isNull(resourceRequestsMap)) {
        logger.warn("The resource request for the container with name={} is missing.", k8sContainer.getName());
      } else {
        totalCpuRequestAmount += K8sResourceUtils.getCpuResourceRequest(k8sContainer);
        totalMemoryRequestAmount += K8sResourceUtils.getMemResourceRequest(k8sContainer);
      }
    }

    Builder cpuRequestBuilder = Resource.Quantity.newBuilder();
    cpuRequestBuilder.setAmount(Float.toString(totalCpuRequestAmount));
    cpuRequestBuilder.setUnit(totalCpuRequestUnit);

    Builder memRequestBuilder = Resource.Quantity.newBuilder();
    memRequestBuilder.setAmount(Float.toString(totalMemoryRequestAmount));
    memRequestBuilder.setUnit(totalMemoryRequestUnit);

    totalResourceBuilder.putRequests("cpu", cpuRequestBuilder.build()).putRequests("memory", memRequestBuilder.build());
    return totalResourceBuilder.build();
  }

  public static float getCpuResourceRequest(io.fabric8.kubernetes.api.model.Container k8sContainer) {
    float cpuRequestValue = 0;
    String cpuRequestUnit = "";
    Map<String, Quantity> resourceRequestsMap = k8sContainer.getResources().getRequests();
    io.fabric8.kubernetes.api.model.Quantity cpuResourceRequest = resourceRequestsMap.get("cpu");
    if (isNull(cpuResourceRequest)) {
      logger.error("The cpu resource request for the container with name={} is missing.", k8sContainer.getName());
    } else {
      String cpuRequestAmount = cpuResourceRequest.getAmount();
      if (!StringUtils.isBlank(cpuRequestAmount)) {
        // aggregate the requested cpu resource
        cpuRequestValue = Float.valueOf(getResourceValue(cpuRequestAmount));
        cpuRequestUnit = getResourceValue(cpuRequestAmount); // TODO: cpu unit could be "" or "m"
      }

      String cpuRequestFormat = cpuResourceRequest.getFormat();
      if (!StringUtils.isBlank(cpuRequestFormat)) {
        // the format for cpu is the same
        logger.warn("The requested cpu resource has format of {}", cpuRequestFormat);
      }
    }
    return cpuRequestValue;
  }

  public static float getMemResourceRequest(io.fabric8.kubernetes.api.model.Container k8sContainer) {
    float memRequestValue = 0;
    String memRequestUnit = "";
    Map<String, io.fabric8.kubernetes.api.model.Quantity> resourceRequestsMap =
        k8sContainer.getResources().getRequests();
    io.fabric8.kubernetes.api.model.Quantity memResourceRequest = resourceRequestsMap.get("memory");
    if (isNull(memResourceRequest)) {
      logger.error("The memory resource request for the container with name={} is missing.", k8sContainer.getName());
    } else {
      String memRequestAmount = memResourceRequest.getAmount();
      if (!StringUtils.isBlank(memRequestAmount)) {
        memRequestValue = Float.valueOf(getResourceValue(memRequestAmount));
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
