package software.wings.api.commandlibrary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
public class CommandLibraryConfigurationDTO {
  List<String> supportedCommandStoreIdList;
  int clImplementationVersion;

  @Builder
  public CommandLibraryConfigurationDTO(List<String> supportedCommandStoreIdList, int clImplementationVersion) {
    this.supportedCommandStoreIdList = supportedCommandStoreIdList;
    this.clImplementationVersion = clImplementationVersion;
  }
}
