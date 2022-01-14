package io.harness.commandlibrary.server.beans;

import io.harness.commandlibrary.server.utils.JsonSerializable;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ServiceSecretConfig implements JsonSerializable {
  String managerToCommandLibraryServiceSecret;
}
