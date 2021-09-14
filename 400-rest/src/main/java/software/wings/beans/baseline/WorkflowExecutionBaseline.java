/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.baseline;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;

import software.wings.beans.Base;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

@Data
@Builder
@EqualsAndHashCode(callSuper = false, exclude = {"workflowExecutionId"})
@FieldNameConstants(innerTypeName = "WorkflowExecutionBaselineKeys")
@Entity(value = "workflowExecutionBaselines", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class WorkflowExecutionBaseline extends Base implements AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_baseline")
                 .unique(true)
                 .field(WorkflowExecutionBaselineKeys.workflowId)
                 .field(WorkflowExecutionBaselineKeys.envId)
                 .field(WorkflowExecutionBaselineKeys.serviceId)
                 .build())
        .build();
  }

  public static final String WORKFLOW_ID_KEY = "workflowId";
  public static final String ENV_ID_KEY = "envId";
  public static final String SERVICE_ID_KEY = "serviceId";

  @NotEmpty private String workflowId;
  @NotEmpty private String envId;
  @NotEmpty private String serviceId;
  @NotEmpty @FdIndex private String workflowExecutionId;
  @FdIndex private String accountId;
  private String pipelineExecutionId;

  @UtilityClass
  public static class WorkflowExecutionBaselineKeys {
    // Temporary
    public static final String appId = "appId";
  }
}
