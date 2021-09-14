/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api.commandlibrary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
public class CommandLibraryConfigurationDTO {
  List<String> supportedCommandStoreNameList;
  int clImplementationVersion;

  @Builder
  public CommandLibraryConfigurationDTO(List<String> supportedCommandStoreNameList, int clImplementationVersion) {
    this.supportedCommandStoreNameList = supportedCommandStoreNameList;
    this.clImplementationVersion = clImplementationVersion;
  }
}
