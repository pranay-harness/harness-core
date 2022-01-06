/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.Expression;
import io.harness.expression.ExpressionReflectionUtils.NestedAnnotationResolver;

import com.github.reinert.jjschema.Attributes;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Builder
@FieldNameConstants(innerTypeName = "AwsInstanceFilterKeys")
@OwnedBy(CDP)
public class AwsInstanceFilter implements NestedAnnotationResolver {
  @Attributes(title = "VPC") private List<String> vpcIds = new ArrayList<>();
  @Attributes(title = "Tags") @Expression(ALLOW_SECRETS) private List<Tag> tags = new ArrayList<>();

  /**
   * Gets vpc ids.
   *
   * @return the vpc ids
   */
  public List<String> getVpcIds() {
    return vpcIds;
  }

  /**
   * Sets vpc ids.
   *
   * @param vpcIds the vpc ids
   */
  public void setVpcIds(List<String> vpcIds) {
    this.vpcIds = vpcIds;
  }

  public List<Tag> getTags() {
    return tags;
  }

  public void setTags(List<Tag> tags) {
    this.tags = tags;
  }

  @Data
  @Builder
  public static class Tag implements NestedAnnotationResolver {
    @Expression(ALLOW_SECRETS) private String key;
    @Expression(ALLOW_SECRETS) private String value;
  }
}
