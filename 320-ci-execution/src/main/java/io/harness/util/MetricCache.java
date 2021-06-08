package io.harness.util;

import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class MetricCache {
  // accountId:orgId:projectId:pipelineId:stageId:stepId -> [201, 210, 215...]
  private final Map<String, ArrayList<Integer>> maxMemoryMibCache = new ConcurrentHashMap<>();

  // accountId:orgId:projectId:pipelineId:stageId:stepId -> [105, 110, 115...]
  private final Map<String, ArrayList<Integer>> maxMilliCpuCache = new ConcurrentHashMap<>();

  // accountId:orgId:projectId:pipelineId:stageId:stepId -> 105
  private final Map<String, Integer> currMemoryMibCache = new ConcurrentHashMap<>();
  // accountId:orgId:projectId:pipelineId:stageId:stepId -> 101
  private final Map<String, Integer> currMilliCpuCache = new ConcurrentHashMap<>();

  // accountId:orgId:projectId:pipelineId -> [stageId:stepId, ...]
  private final Map<String, ArrayList<String>> currPipelineStepIds = new ConcurrentHashMap<>();

  public int getMaxStepMemoryMib(
      String accountId, String orgId, String projectId, String pipelineId, String stageId, String stepId) {
    String key = getKey(accountId, orgId, projectId, pipelineId, stageId, stepId);
    List<Integer> l = maxMemoryMibCache.get(key);
    return Collections.max(l);
  }

  public int getMaxStepMilliCpu(
      String accountId, String orgId, String projectId, String pipelineId, String stageId, String stepId) {
    String key = getKey(accountId, orgId, projectId, pipelineId, stageId, stepId);
    List<Integer> l = maxMilliCpuCache.get(key);
    return Collections.max(l);
  }

  public int getCurrStepMilliCpu(
      String accountId, String orgId, String projectId, String pipelineId, String stageId, String stepId) {
    String key = getKey(accountId, orgId, projectId, pipelineId, stageId, stepId);
    return currMilliCpuCache.get(key);
  }

  public int getCurrStepMemoryMib(
      String accountId, String orgId, String projectId, String pipelineId, String stageId, String stepId) {
    String key = getKey(accountId, orgId, projectId, pipelineId, stageId, stepId);
    return currMemoryMibCache.get(key);
  }

  public List<String> getPipelineSteps(String accountId, String orgId, String projectId, String pipelineId) {
    return currPipelineStepIds.get(String.format("%s:%s:%s:%s", accountId, orgId, projectId, pipelineId));
  }

  private String getKey(
      String accountId, String orgId, String projectId, String pipelineId, String stageId, String stepId) {
    return String.format("%s:%s:%s:%s:%s:%s", accountId, orgId, projectId, pipelineId, stageId, stepId);
  }

  private void populateMaxStepMemory(String key, Integer memory) {
    if (!maxMemoryMibCache.containsKey(key)) {
      maxMemoryMibCache.put(key, new ArrayList<>());
    }

    maxMemoryMibCache.get(key).add(memory);
  }

  private void populateMaxStepCpu(String key, Integer cpu) {
    if (!maxMilliCpuCache.containsKey(key)) {
      maxMilliCpuCache.put(key, new ArrayList<>());
    }

    maxMilliCpuCache.get(key).add(cpu);
  }

  private void populatePipelineStep(
      String accountId, String orgId, String projectId, String pipelineId, String stageId, String stepId) {
    String key = String.format("%s:%s:%s:%s", accountId, orgId, projectId, pipelineId);
    String val = String.format("%s:%s", stageId, stepId);

    if (!currPipelineStepIds.containsKey(key)) {
      currPipelineStepIds.put(key, new ArrayList<>());
    }

    List<String> s = currPipelineStepIds.get(key);
    if (!s.contains(val)) {
      currPipelineStepIds.get(key).add(val);
    }
  }

  public void populate(String accountId, String orgId, String projectId, String pipelineId, String stageId,
      String stepId, Integer maxMemory, Integer maxCpu, Integer currMemory, Integer currCpu) {
    String key = getKey(accountId, orgId, projectId, pipelineId, stageId, stepId);
    currMemoryMibCache.put(key, currMemory);
    currMilliCpuCache.put(key, currCpu);
    populateMaxStepMemory(key, maxMemory);
    populateMaxStepCpu(key, maxCpu);
    populatePipelineStep(accountId, orgId, projectId, pipelineId, stageId, stepId);
  }
}
