package io.harness.accesscontrol.acl.models;

import static io.harness.data.structure.HasPredicate.hasNone;

import io.harness.resourcegroup.model.DynamicResourceSelector;
import io.harness.resourcegroup.model.ResourceSelector;
import io.harness.resourcegroup.model.StaticResourceSelector;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HResource {
  public static final String IDENTIFIER_FOR_ALL_RESOURCES = "*";
  private String resourceIdentifier;
  private String resourceType;

  public String getResourceIdentifier() {
    return hasNone(resourceIdentifier) ? IDENTIFIER_FOR_ALL_RESOURCES : resourceIdentifier;
  }

  public static List<Optional<HResource>> fromResourceSelector(ResourceSelector resourceSelector) {
    if (resourceSelector instanceof StaticResourceSelector) {
      StaticResourceSelector staticResourceSelector = (StaticResourceSelector) resourceSelector;
      return staticResourceSelector.getIdentifiers()
          .stream()
          .map(identifier
              -> Optional.ofNullable(HResource.builder()
                                         .resourceIdentifier(identifier)
                                         .resourceType(staticResourceSelector.getResourceType())
                                         .build()))
          .collect(Collectors.toList());
    } else if (resourceSelector instanceof DynamicResourceSelector) {
      return Collections.singletonList(
          Optional.ofNullable(HResource.builder()
                                  .resourceIdentifier(IDENTIFIER_FOR_ALL_RESOURCES)
                                  .resourceType(((DynamicResourceSelector) resourceSelector).getResourceType())
                                  .build()));
    }
    return Collections.singletonList(Optional.empty());
  }
}
