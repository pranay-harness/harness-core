package io.harness.accesscontrol.resources;

import io.harness.accesscontrol.resources.resourcetypes.ResourceType;
import io.harness.accesscontrol.resources.resourcetypes.ResourceTypeService;
import io.harness.exception.InvalidRequestException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class ResourceTypeManagementJob {
  private static final String RESOURCE_TYPES_YAML_PATH = "io/harness/accesscontrol/resources/resourceTypes.yml";

  private final ResourceTypeService resourceTypeService;
  private final Set<ResourceType> latestResourceTypes;
  private final Set<ResourceType> currentResourceTypes;

  @Inject
  public ResourceTypeManagementJob(ResourceTypeService resourceTypeService) {
    this.resourceTypeService = resourceTypeService;
    this.currentResourceTypes = new HashSet<>(resourceTypeService.list());
    this.latestResourceTypes = fetchResourceTypesFromYaml();
  }

  private Set<ResourceType> fetchResourceTypesFromYaml() {
    ObjectMapper om = new ObjectMapper(new YAMLFactory());
    URL url = getClass().getClassLoader().getResource(RESOURCE_TYPES_YAML_PATH);
    try {
      byte[] bytes = Resources.toByteArray(url);
      return om.readValue(bytes, new TypeReference<Set<ResourceType>>() {});
    } catch (IOException e) {
      throw new InvalidRequestException("Resource type file path or file is invalid");
    }
  }

  public void run() {
    Set<ResourceType> newResourceTypes = Sets.difference(latestResourceTypes, currentResourceTypes);
    newResourceTypes.forEach(resourceTypeService::save);
  }
}
