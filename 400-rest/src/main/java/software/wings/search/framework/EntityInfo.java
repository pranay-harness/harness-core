package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

/**
 * The mini view of entities included
 * in a search preview.
 *
 * @author utkarsh
 */
@OwnedBy(PL)
@Value
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "EntityInfoKeys")
public class EntityInfo {
  String id;
  String name;
}
