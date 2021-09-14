/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.remote;

import static io.harness.rule.OwnerRule.KARAN;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class OrganizationMapperTest extends CategoryTest {
  OrganizationDTO organizationDTO;
  Organization organization;

  @Before
  public void setUp() {
    organizationDTO = OrganizationDTO.builder().identifier(randomAlphabetic(10)).name(randomAlphabetic(10)).build();
    organization = Organization.builder()
                       .accountIdentifier(randomAlphabetic(10))
                       .identifier(randomAlphabetic(10))
                       .name(randomAlphabetic(10))
                       .description("")
                       .tags(emptyList())
                       .lastModifiedAt(10L)
                       .build();
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testToOrganization() {
    Organization fromDTO = OrganizationMapper.toOrganization(organizationDTO);
    assertNotNull(fromDTO);
    assertEquals(organizationDTO.getIdentifier(), fromDTO.getIdentifier());
    assertEquals(organizationDTO.getName(), fromDTO.getName());
    assertNotNull(fromDTO.getTags());
    assertNotNull(fromDTO.getDescription());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testWriteDTO() {
    OrganizationDTO fromOrganization = OrganizationMapper.writeDto(organization);
    assertEquals(organization.getIdentifier(), fromOrganization.getIdentifier());
    assertEquals(organization.getName(), fromOrganization.getName());
    assertEquals(organization.getDescription(), fromOrganization.getDescription());
    assertEquals(emptyMap(), fromOrganization.getTags());
  }
}
