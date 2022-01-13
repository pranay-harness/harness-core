/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.schema.query;

import graphql.schema.DataFetchingFieldSelectionSet;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLCloudProvidersQueryParameters implements QLPageQueryParameters {
  private String accountId;
  private int limit;
  private int offset;

  private DataFetchingFieldSelectionSet selectionSet;
}
