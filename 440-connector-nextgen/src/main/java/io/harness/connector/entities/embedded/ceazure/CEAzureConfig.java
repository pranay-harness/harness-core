/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.entities.embedded.ceazure;

import io.harness.connector.entities.Connector;
import io.harness.delegate.beans.connector.CEFeatures;

import java.util.List;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@Persistent
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "CEAzureConfigKeys")
@Entity(value = "connectors", noClassnameStored = true)
@TypeAlias("io.harness.connector.entities.embedded.ceazure.CEAzureConfig")
public class CEAzureConfig extends Connector {
  @NotEmpty List<CEFeatures> featuresEnabled;
  @NotNull String subscriptionId;
  @NotNull String tenantId;
  @Nullable BillingExportDetails billingExportDetails;
}
