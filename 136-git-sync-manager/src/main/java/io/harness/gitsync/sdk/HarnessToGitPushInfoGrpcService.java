package io.harness.gitsync.sdk;

import io.harness.gitsync.FileInfo;
import io.harness.gitsync.HarnessToGitPushInfoServiceGrpc.HarnessToGitPushInfoServiceImplBase;
import io.harness.gitsync.InfoForPush;
import io.harness.gitsync.PushInfo;
import io.harness.gitsync.PushResponse;
import io.harness.gitsync.common.service.HarnessToGitHelperService;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitydetail.EntityDetailProtoToRestMapper;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;

@Singleton
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
    io.harness.gitsync.common.beans.InfoForPush infoForPush =
        harnessToGitHelperService.getInfoForPush(request.getYamlGitConfigId(), request.getBranch(),
            request.getFilePath(), request.getAccountId(), entityDetailDTO.getEntityRef(), entityDetailDTO.getType());
    final ByteString connector = ByteString.copyFrom(kryoSerializer.asBytes(infoForPush.getScmConnector()));
    responseObserver.onNext(
        InfoForPush.newBuilder().setConnector(connector).setFilePath(infoForPush.getFilePath()).build());
    responseObserver.onCompleted();
  }
}
