/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.schema.mutation.secretManager;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLCustomSecretManagerInput extends QLSecretManagerInput {
  String name;
  String templateId;
  Set<String> delegateSelectors;
  Set<QLEncryptedDataParams> testVariables;
  boolean executeOnDelegate;
  boolean isConnectorTemplatized;
  String host;
  String commandPath;
  String connectorId;
}
