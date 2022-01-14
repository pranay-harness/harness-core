package io.harness.delegate.task.citasks.cik8handler;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ImageCredentials {
  String userName;
  String password;
  String registryUrl;
}
