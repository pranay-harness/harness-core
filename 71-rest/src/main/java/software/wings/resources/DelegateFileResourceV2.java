package software.wings.resources;

import static java.time.Duration.ofHours;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.delegatetasks.DelegateFile.Builder.aDelegateFile;
import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE;

import com.google.common.io.Files;
import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.data.structure.UUIDGenerator;
import io.harness.distribution.idempotence.IdempotentId;
import io.harness.distribution.idempotence.IdempotentLock;
import io.harness.distribution.idempotence.IdempotentResult;
import io.harness.rest.RestResponse;
import io.harness.stream.BoundedInputStream;
import io.swagger.annotations.Api;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.app.MainConfiguration;
import software.wings.beans.FileMetadata;
import software.wings.common.MongoIdempotentRegistry;
import software.wings.delegatetasks.DelegateFile;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;

import java.io.File;
import java.io.InputStream;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.StreamingOutput;

@Api("agent/delegateFiles")
@Path("agent/delegateFiles")
@Produces("application/json")
@Scope(DELEGATE)
@Slf4j
public class DelegateFileResourceV2 {
  @Inject private FileService fileService;
  @Inject private MainConfiguration configuration;
  @Inject private ConfigService configService;
  @Inject private DelegateService delegateService;

  @Value
  @Builder
  public static class FileIdempotentResult implements IdempotentResult {
    private String fileId;
  }

  @Inject MongoIdempotentRegistry<DelegateFileResource.FileIdempotentResult> idempotentRegistry;

  @DelegateAuth
  @POST
  @Path("{delegateId}/tasks/{taskId}")
  @Consumes(MULTIPART_FORM_DATA)
  @Timed
  @ExceptionMetered
  public RestResponse<String> upload(@PathParam("delegateId") String delegateId, @PathParam("taskId") String taskId,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("fileBucket") FileBucket fileBucket,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    logger.info("Received save artifact request for delegateId : {}, taskId: {}, accountId: {}, fileDetail: {}",
        delegateId.replaceAll("[\r\n]", ""), taskId.replaceAll("[\r\n]", ""), accountId.replaceAll("[\r\n]", ""),
        fileDetail.toString().replaceAll("[\r\n]", ""));

    // TODO: Do more check, so one delegate does not overload system

    IdempotentId idempotentid = new IdempotentId(taskId + ":" + fileDetail.getFileName());

    try (IdempotentLock<DelegateFileResource.FileIdempotentResult> idempotent =
             idempotentRegistry.create(idempotentid, ofMinutes(1), ofSeconds(1), ofHours(2))) {
      if (idempotent.alreadyExecuted()) {
        return new RestResponse<>(idempotent.getResult().getFileId());
      }
      FileMetadata fileMetadata = FileMetadata.builder()
                                      .fileName(new File(fileDetail.getFileName()).getName())
                                      .accountId(accountId)
                                      .fileUuid(UUIDGenerator.generateUuid())
                                      .build();
      String fileId = fileService.saveFile(fileMetadata,
          new BoundedInputStream(uploadedInputStream, configuration.getFileUploadLimits().getAppContainerLimit()),
          fileBucket);
      logger.info("fileId: {} and fileName {}", fileId, fileMetadata.getFileName());

      idempotent.succeeded(DelegateFileResource.FileIdempotentResult.builder().fileId(fileId).build());
      return new RestResponse<>(fileId);
    }
  }

  @DelegateAuth
  @GET
  @Path("fileId")
  @Timed
  @ExceptionMetered
  public RestResponse<String> getFileId(@QueryParam("entityId") @NotEmpty String entityId,
      @QueryParam("fileBucket") @NotNull FileBucket fileBucket, @QueryParam("version") int version,
      @QueryParam("accountId") @NotEmpty String accountId) {
    logger.debug("entityId: {}, fileBucket: {}, version: {}", entityId.replaceAll("[\r\n]", ""),
        fileBucket.toString().replaceAll("[\r\n]", ""), version);
    return new RestResponse<>(fileService.getFileIdByVersion(entityId, version, fileBucket));
  }

  @DelegateAuth
  @GET
  @Path("downloadConfig")
  @Timed
  @ExceptionMetered
  public StreamingOutput downloadConfigFile(@QueryParam("fileId") @NotEmpty String fileId,
      @QueryParam("appId") @NotEmpty String appId, @QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("activityId") @NotEmpty String activityId) {
    return output -> {
      File configFile = configService.downloadForActivity(appId, fileId, activityId);
      byte[] bytes = Files.toByteArray(configFile);
      output.write(bytes, 0, bytes.length);
      output.flush();
    };
  }

  @DelegateAuth
  @GET
  @Path("download")
  @Timed
  @ExceptionMetered
  public StreamingOutput downloadFile(@QueryParam("fileId") @NotEmpty String fileId,
      @QueryParam("fileBucket") @NotNull FileBucket fileBucket, @QueryParam("accountId") @NotEmpty String accountId) {
    logger.info(
        "fileId: {}, fileBucket: {}", fileId.replaceAll("[\r\n]", ""), fileBucket.toString().replaceAll("[\r\n]", ""));
    return output -> fileService.downloadToStream(fileId, output, fileBucket);
  }

  @DelegateAuth
  @GET
  @Path("metainfo")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateFile> getFileInfo(@QueryParam("fileId") String fileId,
      @QueryParam("fileBucket") @NotNull FileBucket fileBucket, @QueryParam("accountId") @NotEmpty String accountId) {
    logger.info(
        "fileId: {}, fileBucket: {}", fileId.replaceAll("[\r\n]", ""), fileBucket.toString().replaceAll("[\r\n]", ""));

    FileMetadata fileMetadata = fileService.getFileMetadata(fileId, fileBucket);

    return new RestResponse<>(aDelegateFile()
                                  .withFileId(fileId)
                                  .withBucket(fileBucket)
                                  .withFileName(fileMetadata.getFileName())
                                  .withLength(fileMetadata.getFileLength())
                                  .build());
  }

  @DelegateAuth
  @POST
  @Path("{delegateId}/profile-result")
  @Timed
  @ExceptionMetered
  public void saveProfileResult(@PathParam("delegateId") String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("error") boolean error,
      @QueryParam("fileBucket") FileBucket fileBucket, @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    delegateService.saveProfileResult(accountId, delegateId, error, fileBucket, uploadedInputStream, fileDetail);
  }
}
