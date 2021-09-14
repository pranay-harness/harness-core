/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.exception;

import io.harness.exception.HarnessException;

import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author rktummala on 12/18/17
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class YamlProcessingException extends HarnessException {
  private Map<String, ChangeWithErrorMsg> failedYamlFileChangeMap;
  private List<ChangeContext> changeContextList;
  private List<Change> changeList;

  public YamlProcessingException(String message, Map<String, ChangeWithErrorMsg> failedYamlFileChangeMap) {
    super(message);
    this.failedYamlFileChangeMap = failedYamlFileChangeMap;
  }

  public YamlProcessingException(
      String message, Throwable cause, Map<String, ChangeWithErrorMsg> failedYamlFileChangeMap) {
    super(message, cause);
    this.failedYamlFileChangeMap = failedYamlFileChangeMap;
  }

  public YamlProcessingException(String message, Map<String, ChangeWithErrorMsg> failedYamlFileChangeMap,
      List<ChangeContext> changeContextList, List<Change> changeList) {
    super(message);
    this.failedYamlFileChangeMap = failedYamlFileChangeMap;
    this.changeContextList = changeContextList;
    this.changeList = changeList;
  }

  @Data
  @lombok.Builder
  public static class ChangeWithErrorMsg {
    private Change change;
    private String errorMsg;
  }
}
