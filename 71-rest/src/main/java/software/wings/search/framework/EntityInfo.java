package software.wings.search.framework;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

/**
 * The mini view of entities included
 * in a search preview.
 *
 * @author utkarsh
 */
@Value
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "EntityInfoKeys")
public class EntityInfo {
  String id;
  String name;
}
