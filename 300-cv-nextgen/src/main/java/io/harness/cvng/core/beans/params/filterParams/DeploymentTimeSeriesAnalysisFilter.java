/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.beans.params.filterParams;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import java.util.List;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Data
@NoArgsConstructor
public class DeploymentTimeSeriesAnalysisFilter extends TimeSeriesAnalysisFilter {
  // currently not removing hostName for backward compatibility
  // TODO: remove String hostName and only use List<String> hostNameList
  @QueryParam("hostName") @Deprecated String hostName;
  @QueryParam("hostNames") List<String> hostNames;
  @QueryParam("transactionNames") List<String> transactionNames;

  public boolean filterByHostName() {
    return isNotEmpty(hostName);
  }

  public boolean filterByHostNames() {
    return isNotEmpty(hostNames);
  }

  public boolean filterByTransactionNames() {
    return isNotEmpty(transactionNames);
  }
}
