/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
