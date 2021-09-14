/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.helpers.ext.azure;

import lombok.Data;

@Data
public class AksGetCredentialsResponse {
  private String id;
  private String location;
  private String name;
  private String type;
  private AksGetCredentialProperties properties;

  @Data
  public class AksGetCredentialProperties {
    private String kubeConfig;
  }
}
