/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.utils;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAGHU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.client.NextGenClient;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.client.NextGenServiceImpl.EntityKey;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.OrganizationResponse;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ProjectResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.environment.dto.EnvironmentResponse;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.rule.Owner;

import com.google.common.cache.CacheLoader;
import com.google.inject.Inject;
import java.io.IOException;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import retrofit2.Call;
import retrofit2.Response;

public class NextGenServiceImplTest extends CvNextGenTestBase {
  @Inject private NextGenService nextGenService;
  @Mock private NextGenClient nextGenClient;
  private String accountId;
  private String orgIdentifier;
  private String projectIdentifier;

  @Before
  public void setup() throws Exception {
    accountId = generateUuid();
    orgIdentifier = generateUuid();
    projectIdentifier = generateUuid();
    FieldUtils.writeField(nextGenService, "nextGenClient", nextGenClient, true);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetEnvironment() throws IOException {
    Call<ResponseDTO<EnvironmentResponse>> call = Mockito.mock(Call.class);
    when(call.clone()).thenReturn(call);
    String envIdentifier = generateUuid();
    when(nextGenClient.getEnvironment(envIdentifier, accountId, orgIdentifier, projectIdentifier)).thenReturn(call);
    when(call.execute())
        .thenReturn(Response.success(ResponseDTO.newResponse(
            EnvironmentResponse.builder()
                .environment(EnvironmentResponseDTO.builder().identifier(envIdentifier).name("env").build())
                .build())));
    EnvironmentResponseDTO environment =
        nextGenService.getEnvironment(accountId, orgIdentifier, projectIdentifier, envIdentifier);
    assertThat(environment).isNotNull();
    assertThat(environment.getIdentifier()).isEqualTo(envIdentifier);
    assertThat(environment.getName()).isEqualTo("env");

    final String newEnvIdentifier = generateUuid();
    when(nextGenClient.getEnvironment(newEnvIdentifier, accountId, orgIdentifier, projectIdentifier)).thenReturn(call);
    when(call.execute()).thenReturn(Response.success(ResponseDTO.newResponse(EnvironmentResponse.builder().build())));
    assertThatThrownBy(
        () -> nextGenService.getEnvironment(accountId, orgIdentifier, projectIdentifier, newEnvIdentifier))
        .isInstanceOf(CacheLoader.InvalidCacheLoadException.class)
        .hasMessage("CacheLoader returned null for key "
            + EntityKey.builder()
                  .accountId(accountId)
                  .orgIdentifier(orgIdentifier)
                  .projectIdentifier(projectIdentifier)
                  .entityIdentifier(newEnvIdentifier)
                  .build()
            + ".");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetService() throws IOException {
    Call<ResponseDTO<ServiceResponseDTO>> call = Mockito.mock(Call.class);
    when(call.clone()).thenReturn(call);
    String serviceIdentifier = generateUuid();
    when(nextGenClient.getService(serviceIdentifier, accountId, orgIdentifier, projectIdentifier)).thenReturn(call);
    when(call.execute())
        .thenReturn(Response.success(ResponseDTO.newResponse(
            ServiceResponseDTO.builder().identifier(serviceIdentifier).name("service").build())));
    ServiceResponseDTO service =
        nextGenService.getService(accountId, orgIdentifier, projectIdentifier, serviceIdentifier);
    assertThat(service).isNotNull();
    assertThat(service.getIdentifier()).isEqualTo(serviceIdentifier);
    assertThat(service.getName()).isEqualTo("service");

    final String newServiceIdentifier = generateUuid();
    when(nextGenClient.getService(newServiceIdentifier, accountId, orgIdentifier, projectIdentifier)).thenReturn(call);
    when(call.execute()).thenReturn(Response.success(ResponseDTO.newResponse(null)));
    assertThatThrownBy(
        () -> nextGenService.getService(accountId, orgIdentifier, projectIdentifier, newServiceIdentifier))
        .isInstanceOf(CacheLoader.InvalidCacheLoadException.class)
        .hasMessage("CacheLoader returned null for key "
            + EntityKey.builder()
                  .accountId(accountId)
                  .orgIdentifier(orgIdentifier)
                  .projectIdentifier(projectIdentifier)
                  .entityIdentifier(newServiceIdentifier)
                  .build()
            + ".");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetOrganization() throws IOException {
    Call<ResponseDTO<OrganizationResponse>> call = Mockito.mock(Call.class);
    when(call.clone()).thenReturn(call);
    when(nextGenClient.getOrganization(orgIdentifier, accountId)).thenReturn(call);
    when(call.execute())
        .thenReturn(Response.success(ResponseDTO.newResponse(
            OrganizationResponse.builder()
                .organization(OrganizationDTO.builder().identifier(orgIdentifier).name("orgName").build())
                .build())));
    OrganizationDTO organizationDTO = nextGenService.getOrganization(accountId, this.orgIdentifier);
    assertThat(organizationDTO).isNotNull();
    assertThat(organizationDTO.getIdentifier()).isEqualTo(orgIdentifier);
    assertThat(organizationDTO.getName()).isEqualTo("orgName");

    final String newOrgIdentifier = generateUuid();
    when(nextGenClient.getOrganization(newOrgIdentifier, accountId)).thenReturn(call);
    when(call.execute()).thenReturn(Response.success(ResponseDTO.newResponse(OrganizationResponse.builder().build())));
    assertThatThrownBy(() -> nextGenService.getOrganization(accountId, newOrgIdentifier))
        .isInstanceOf(CacheLoader.InvalidCacheLoadException.class)
        .hasMessage("CacheLoader returned null for key "
            + EntityKey.builder().accountId(accountId).orgIdentifier(newOrgIdentifier).build() + ".");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetCachedProject() throws IOException {
    Call<ResponseDTO<ProjectResponse>> call = Mockito.mock(Call.class);
    when(call.clone()).thenReturn(call);
    when(nextGenClient.getProject(projectIdentifier, accountId, orgIdentifier)).thenReturn(call);
    when(call.execute())
        .thenReturn(Response.success(ResponseDTO.newResponse(
            ProjectResponse.builder()
                .project(ProjectDTO.builder().identifier(projectIdentifier).name("projectName").build())
                .build())));
    ProjectDTO projectDTO = nextGenService.getCachedProject(accountId, orgIdentifier, projectIdentifier);
    assertThat(projectDTO).isNotNull();
    assertThat(projectDTO.getIdentifier()).isEqualTo(projectIdentifier);
    assertThat(projectDTO.getName()).isEqualTo("projectName");

    final String newProjectIdentifier = generateUuid();
    when(nextGenClient.getProject(newProjectIdentifier, accountId, orgIdentifier)).thenReturn(call);
    when(call.execute()).thenReturn(Response.success(ResponseDTO.newResponse(ProjectResponse.builder().build())));
    assertThatThrownBy(() -> nextGenService.getCachedProject(accountId, orgIdentifier, newProjectIdentifier))
        .isInstanceOf(CacheLoader.InvalidCacheLoadException.class)
        .hasMessage("CacheLoader returned null for key "
            + EntityKey.builder()
                  .accountId(accountId)
                  .orgIdentifier(orgIdentifier)
                  .projectIdentifier(newProjectIdentifier)
                  .build()
            + ".");
  }
}
