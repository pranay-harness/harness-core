package io.harness.connector.apis.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.apis.dto.ConnectorFilterDTO;
import io.harness.connector.services.ConnectorFilterService;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.utils.PageTestUtils;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;

public class ConnectorFilterResourceTest extends CategoryTest {
  @InjectMocks ConnectorFilterResource connectorFilterResource;
  @Mock ConnectorFilterService connectorFilterService;
  private static final String accountIdentifier = "accountIdentifier";
  private static final String orgIdentifier = "orgIdentifier";
  private static final String projectIdentifier = "projectIdentifier";
  private static final String identifier = "identifier";
  private ConnectorFilterDTO connectorFilterDTO;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    connectorFilterDTO = ConnectorFilterDTO.builder().build();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testGet() {
    when(connectorFilterService.get(any(), any(), any(), any())).thenReturn(connectorFilterDTO);
    ConnectorFilterDTO connectorFilterOutput =
        connectorFilterResource.get("accountIdentifier", "orgIdentifier", "projectIdentifier", "connectorIdentifier")
            .getData();
    Mockito.verify(connectorFilterService, times(1)).get(any(), any(), any(), any());
    assertThat(connectorFilterOutput).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testList() {
    final Page<ConnectorFilterDTO> page = PageTestUtils.getPage(Arrays.asList(ConnectorFilterDTO.builder().build()), 1);
    when(connectorFilterService.list(0, 100, accountIdentifier, orgIdentifier, projectIdentifier, null))
        .thenReturn(page);
    ResponseDTO<PageResponse<ConnectorFilterDTO>> connectorFilterList =
        connectorFilterResource.list(0, 100, accountIdentifier, orgIdentifier, projectIdentifier);
    Mockito.verify(connectorFilterService, times(1))
        .list(eq(0), eq(100), eq(accountIdentifier), eq(orgIdentifier), eq(projectIdentifier), eq(null));
    assertThat(connectorFilterList.getData()).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testCreate() {
    doReturn(ConnectorFilterDTO.builder().build()).when(connectorFilterService).create(any(), any());
    ResponseDTO<ConnectorFilterDTO> connectorFilterOutput =
        connectorFilterResource.create(ConnectorFilterDTO.builder().build(), accountIdentifier);
    Mockito.verify(connectorFilterService, times(1)).create(any(), any());
    assertThat(connectorFilterOutput.getData()).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testUpdate() {
    doReturn(ConnectorFilterDTO.builder().build()).when(connectorFilterService).update(any(), any());
    ResponseDTO<ConnectorFilterDTO> connectorFilterOutput =
        connectorFilterResource.update(ConnectorFilterDTO.builder().build(), accountIdentifier);
    Mockito.verify(connectorFilterService, times(1)).update(any(), any());
    assertThat(connectorFilterOutput.getData()).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testDelete() {
    when(connectorFilterService.delete(any(), any(), any(), any())).thenReturn(true);
    ResponseDTO<Boolean> result = connectorFilterResource.delete(
        "accountIdentifier", "orgIdentifier", "projectIdentifier", "connectorIdentifier");
    Mockito.verify(connectorFilterService, times(1)).delete(any(), any(), any(), any());
    assertThat(result.getData()).isTrue();
  }
}