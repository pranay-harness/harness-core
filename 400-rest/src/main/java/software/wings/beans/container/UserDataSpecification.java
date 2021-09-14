/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.container;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.FdUniqueIndex;

import software.wings.beans.DeploymentSpecification;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

/**
 * Created by anubhaw on 12/18/17.
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "UserDataSpecificationKeys")
@Entity("userDataSpecifications")
@HarnessEntity(exportable = true)
public class UserDataSpecification extends DeploymentSpecification {
  @NotEmpty @FdUniqueIndex private String serviceId;
  @NotNull private String data;

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends DeploymentSpecification.Yaml {
    private String data;

    @Builder
    public Yaml(String type, String harnessApiVersion, String data) {
      super(type, harnessApiVersion);
      this.data = data;
    }
  }
}
