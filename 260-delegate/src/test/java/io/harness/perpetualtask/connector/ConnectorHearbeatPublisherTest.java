package io.harness.perpetualtask.connector;

import static io.harness.ng.core.activityhistory.NGActivityStatus.SUCCESS;
import static io.harness.ng.core.activityhistory.NGActivityType.CONNECTIVITY_CHECK;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doReturn;

import io.harness.CategoryTest;
import io.harness.beans.IdentifierRef;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.ConnectorHeartbeatDelegateResponse;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.entityactivity.remote.EntityActivityClient;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import retrofit2.Call;
import retrofit2.Response;

public class ConnectorHearbeatPublisherTest extends CategoryTest {
  @InjectMocks ConnectorHearbeatPublisher connectorHearbeatPublisher;
  @Mock EntityActivityClient entityActivityClient;
  private static final String accountId = "accountId";
  @Mock private Call<ResponseDTO<NGActivityDTO>> call;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    when(entityActivityClient.save(any())).thenReturn(call);
    doReturn(Response.success(ResponseDTO.newResponse())).when(call).execute();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testPushConnectivityCheckActivity() {
    final String orgIdentifier = "orgIdentifier";
    final String projectIdentifier = "projectIdentifier";
    final String connectorIdentifier = "connectorIdentifier";
    final String connectorName = "connectorName";
    final String errorMessage = "errorMessage";
    final long testedAtTime = System.currentTimeMillis();
    ConnectorValidationResult connectorValidationResult =
        ConnectorValidationResult.builder().errorMessage(errorMessage).valid(true).testedAt(testedAtTime).build();
    ConnectorHeartbeatDelegateResponse connectorHeartbeatDelegateResponse =
        ConnectorHeartbeatDelegateResponse.builder()
            .accountIdentifier(accountId)
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .identifier(connectorIdentifier)
            .name(connectorName)
            .connectorValidationResult(connectorValidationResult)
            .build();
    connectorHearbeatPublisher.pushConnectivityCheckActivity(accountId, connectorHeartbeatDelegateResponse);
    ArgumentCaptor<NGActivityDTO> argumentCaptor = ArgumentCaptor.forClass(NGActivityDTO.class);
    verify(entityActivityClient, times(1)).save(argumentCaptor.capture());
    NGActivityDTO ngActivityDTO = argumentCaptor.getValue();
    assertThat(ngActivityDTO).isNotNull();
    assertThat(ngActivityDTO.getType()).isEqualTo(CONNECTIVITY_CHECK);
    assertThat(ngActivityDTO.getAccountIdentifier()).isEqualTo(accountId);
    assertThat(ngActivityDTO.getActivityStatus()).isEqualTo(SUCCESS);
    assertThat(ngActivityDTO.getActivityTime()).isEqualTo(testedAtTime);
    assertThat(ngActivityDTO.getErrorMessage()).isEqualTo(errorMessage);
    assertThat(ngActivityDTO.getReferredEntity().getType().toString()).isEqualTo("Connectors");
    assertThat(ngActivityDTO.getReferredEntity().getName()).isEqualTo("connectorName");
    IdentifierRef entityReference = (IdentifierRef) ngActivityDTO.getReferredEntity().getEntityRef();
    assertThat(entityReference.getAccountIdentifier()).isEqualTo(accountId);
    assertThat(entityReference.getIdentifier()).isEqualTo(connectorIdentifier);
    assertThat(entityReference.getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(entityReference.getProjectIdentifier()).isEqualTo(projectIdentifier);
  }
}