/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Trimmed;
import io.harness.expression.Expression;
import io.harness.expression.ExpressionReflectionUtils.NestedAnnotationResolver;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Transient;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "GitFileConfigKeys")
@OwnedBy(CDP)
public class GitFileConfig implements NestedAnnotationResolver {
  @Trimmed private String connectorId;
  @Trimmed private String commitId;
  @Trimmed private String branch;
  @Expression(ALLOW_SECRETS) @Trimmed private String filePath;
  @Trimmed @Nullable private String repoName;
  @Expression(ALLOW_SECRETS) private List<String> filePathList;
  @Trimmed @Nullable private String serviceSpecFilePath;
  @Trimmed @Nullable private String taskSpecFilePath;
  private boolean useBranch;
  private boolean useInlineServiceDefinition;
  @Transient @JsonInclude(Include.NON_EMPTY) private String connectorName;
}
