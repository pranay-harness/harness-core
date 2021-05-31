package io.harness.ccm.commons.beans.recommendation;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

/**
 * javax.persistence.Column annotation is jooq aware and can be used while parsing the DB response.
 * https://www.jooq.org/doc/latest/manual/sql-execution/fetching/pojos/#N8DEF1
 */
@Value
@Builder
@OwnedBy(CE)
@FieldNameConstants(innerTypeName = "NodePoolIdKeys")
public class NodePoolId {
  // nodepoolname can be null for some nodes
  String nodepoolname;
  @NonNull String clusterid;
}
