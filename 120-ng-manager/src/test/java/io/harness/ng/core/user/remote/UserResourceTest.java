package io.harness.ng.core.user.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.ARVIND;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.accesscontrol.user.AggregateUserService;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.ng.userprofile.services.api.UserInfoService;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;

@OwnedBy(PL)
public class UserResourceTest {
  private static final String ACCOUNT = "account";
  @Mock private NgUserService ngUserService;
  @Mock private AggregateUserService aggregateUserService;
  @Mock private UserInfoService userInfoService;
  @Inject @InjectMocks private UserResource userResource;

  @Before
  public void setup() throws NoSuchFieldException {
    initMocks(this);
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testGetUserProjectInfo() {
    PageRequest pageRequest = PageRequest.builder().build();
    PageImpl<ProjectDTO> page = new PageImpl<>(
        Arrays.asList(ProjectDTO.builder().build()), org.springframework.data.domain.PageRequest.of(0, 1), 10);
    doReturn(page).when(ngUserService).listProjects(ACCOUNT, pageRequest);
    ResponseDTO<PageResponse<ProjectDTO>> response = userResource.getUserProjectInfo(ACCOUNT, pageRequest);
    assertThat(response).isNotNull();
    PageResponse<ProjectDTO> data = response.getData();
    assertThat(data.getContent()).isEqualTo(page.getContent());
    assertThat(data.getTotalPages()).isEqualTo(10);
    verify(ngUserService).listProjects(ACCOUNT, pageRequest);
  }
}