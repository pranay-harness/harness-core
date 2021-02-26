package io.harness.accesscontrol.acl.models;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.resourcegroup.model.DynamicResourceSelector;
import io.harness.resourcegroup.model.ResourceSelector;
import io.harness.resourcegroup.model.StaticResourceSelector;

import java.util.Optional;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HResource {
  public static final String IDENTIFIER_FOR_ALL_RESOURCES = "*";
  private String resourceIdentifier;
  private String resourceType;

  public String getResourceIdentifier() {
    return isEmpty(resourceIdentifier) ? IDENTIFIER_FOR_ALL_RESOURCES : resourceIdentifier;
  }

  public static Optional<HResource> fromResourceSelector(ResourceSelector resourceSelector) {
    if (resourceSelector instanceof StaticResourceSelector) {
      StaticResourceSelector staticResourceSelector = (StaticResourceSelector) resourceSelector;
      return Optional.ofNullable(HResource.builder()
                                     .resourceIdentifier(staticResourceSelector.getIdentifier())
                                     .resourceType(staticResourceSelector.getResourceType())
                                     .build());
    } else if (resourceSelector instanceof DynamicResourceSelector) {
      return Optional.ofNullable(HResource.builder()
                                     .resourceIdentifier(IDENTIFIER_FOR_ALL_RESOURCES)
                                     .resourceType(((DynamicResourceSelector) resourceSelector).getResourceType())
                                     .build());
    }
    return Optional.empty();
  }
}
