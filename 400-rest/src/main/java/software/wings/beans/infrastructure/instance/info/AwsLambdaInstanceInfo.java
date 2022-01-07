/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.infrastructure.instance.info;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.Tag;
import software.wings.beans.infrastructure.instance.InvocationCount;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CDP)
public class AwsLambdaInstanceInfo extends ServerlessInstanceInfo {
  private String functionName;
  private String version;
  private Set<String> aliases;
  private Set<Tag> tags;
  private String functionArn;
  private String description;
  private String runtime;
  private String handler;

  @Builder
  public AwsLambdaInstanceInfo(List<InvocationCount> invocationCountList, String functionName, String version,
      Set<String> aliases, Set<Tag> tags, String functionArn, String description, String runtime, String handler) {
    super(emptyIfNull(invocationCountList)
              .stream()
              .collect(Collectors.toMap(InvocationCount::getKey, Function.identity())));
    this.functionName = functionName;
    this.version = version;
    this.aliases = aliases;
    this.tags = tags;
    this.functionArn = functionArn;
    this.description = description;
    this.runtime = runtime;
    this.handler = handler;
  }
}
