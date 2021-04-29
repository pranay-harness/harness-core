package io.harness.gitsync.sdk;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.gitsync.FileInfo;
import io.harness.gitsync.HarnessToGitPushInfoServiceGrpc.HarnessToGitPushInfoServiceImplBase;
import io.harness.gitsync.InfoForPush;
import io.harness.gitsync.IsGitSyncEnabled;
import io.harness.gitsync.PushInfo;
import io.harness.gitsync.PushResponse;
import io.harness.gitsync.common.beans.InfoForGitPush;
import io.harness.gitsync.common.service.HarnessToGitHelperService;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitydetail.EntityDetailProtoToRestMapper;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(DX)
public class HarnessToGitPushInfoGrpcService extends HarnessToGitPushInfoServiceImplBase {
  @Inject HarnessToGitHelperService harnessToGitHelperService;
  @Inject KryoSerializer kryoSerializer;
  @Inject EntityDetailProtoToRestMapper entityDetailProtoToRestMapper;

  @Override
  public void pushFromHarness(PushInfo request, StreamObserver<PushResponse> responseObserver) {
    harnessToGitHelperService.postPushOperation(request);
    responseObserver.onNext(PushResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void getConnectorInfo(FileInfo request, StreamObserver<InfoForPush> responseObserver) {
    final EntityDetail entityDetailDTO = entityDetailProtoToRestMapper.createEntityDetailDTO(request.getEntityDetail());
    final InfoForPush.Builder pushInfoBuilder = InfoForPush.newBuilder().setStatus(true);
    try {
      InfoForGitPush infoForPush =
          harnessToGitHelperService.getInfoForPush(request.getYamlGitConfigId(), request.getBranch(),
              request.getFilePath(), request.getAccountId(), entityDetailDTO.getEntityRef(), entityDetailDTO.getType());
      final ByteString connector = ByteString.copyFrom(kryoSerializer.asBytes(infoForPush.getScmConnector()));
      pushInfoBuilder.setConnector(BytesValue.newBuilder().setValue(connector).build())
          .setFilePath(StringValue.newBuilder().setValue(infoForPush.getFilePath()).build())
          .setFolderPath(StringValue.newBuilder().setValue(request.getFolderPath()).build())
          .setOrgIdentifier(StringValue.of(infoForPush.getOrgIdentifier()))
          .setProjectIdentifier(StringValue.of(infoForPush.getProjectIdentifier()))
          .setAccountId(infoForPush.getAccountId())
          .setYamlGitConfigId(infoForPush.getYamlGitConfigId())
          .setIsDefault(infoForPush.isDefault())
          .setDefaultBranchName(infoForPush.getDefaultBranchName())
          .setExecuteOnDelegate(infoForPush.isExecuteOnDelegate());
      if (infoForPush.isExecuteOnDelegate()) {
        final ByteString encyptedDataDetails =
            ByteString.copyFrom(kryoSerializer.asBytes(infoForPush.getEncryptedDataDetailList()));
        pushInfoBuilder.setEncryptedDataDetails(BytesValue.of(encyptedDataDetails));
      }
    } catch (WingsException e) {
      final ByteString exceptionBytes = ByteString.copyFrom(kryoSerializer.asBytes(e.getMessage()));
      pushInfoBuilder.setException(BytesValue.newBuilder().setValue(exceptionBytes).build());
      pushInfoBuilder.setStatus(false);
    } catch (Exception e) {
      log.error("Unknown exception occurred in harness to git grpc.", e);
      pushInfoBuilder.setStatus(false);
      final ByteString exceptionBytes =
          ByteString.copyFrom(kryoSerializer.asBytes(new InvalidRequestException("Unknown exception", e)));
      pushInfoBuilder.setException(BytesValue.newBuilder().setValue(exceptionBytes).build());
    }
    responseObserver.onNext(pushInfoBuilder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void isGitSyncEnabledForScope(EntityScopeInfo request, StreamObserver<IsGitSyncEnabled> responseObserver) {
    final Boolean gitSyncEnabled = harnessToGitHelperService.isGitSyncEnabled(request);
    responseObserver.onNext(IsGitSyncEnabled.newBuilder().setEnabled(gitSyncEnabled).build());
    responseObserver.onCompleted();
  }
}
