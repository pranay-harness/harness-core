/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans.environment.pod;

import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.delegate.beans.ci.pod.PVCParams;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Stores all details require to spawn pod
 */

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PodSetupInfo {
  private PodSetupParams podSetupParams;
  private List<PVCParams> pvcParamsList;
  @NotNull private Map<String, String> volumeToMountPath;
  @NotEmpty private String name;
  @NotNull private Integer stageMemoryRequest;
  @NotNull private Integer stageCpuRequest;
  private List<String> serviceIdList;
  private List<Integer> serviceGrpcPortList;
  @NotEmpty private String workDirPath;

  @Data
  @Builder
  public static final class PodSetupParams {
    private List<ContainerDefinitionInfo> containerDefinitionInfos;
  }
}
