package software.wings.resources;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.delegatetasks.DelegateFile.Builder.aDelegateFile;
import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE;

import com.google.common.io.Files;
import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.mongodb.client.gridfs.model.GridFSFile;
import io.harness.distribution.idempotence.IdempotentId;
import io.harness.distribution.idempotence.IdempotentLock;
import io.harness.distribution.idempotence.UnableToRegisterIdempotentOperationException;
import io.swagger.annotations.Api;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.FileMetadata;
import software.wings.beans.RestResponse;
import software.wings.common.MongoIdempotentRegistry;
import software.wings.delegatetasks.DelegateFile;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.Scope;
import software.wings.security.encryption.EncryptionUtils;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.utils.BoundedInputStream;

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

/**
 * Created by rishi on 12/19/16.
 */
@Api("delegateFiles")
@Path("/delegateFiles")
@Produces("application/json")
@Scope(DELEGATE)
public class DelegateFileResource {
  private static final Logger logger = LoggerFactory.getLogger(DelegateFileResource.class);

  @Inject private FileService fileService;
  @Inject private MainConfiguration configuration;
  @Inject private ConfigService configService;

  @Inject MongoIdempotentRegistry<String> idempotentRegistry;

  @DelegateAuth
  @POST
  @Path("{delegateId}/tasks/{taskId}")
  @Consumes(MULTIPART_FORM_DATA)
  @Timed
  @ExceptionMetered
  public RestResponse<String> upload(@PathParam("delegateId") String delegateId, @PathParam("taskId") String taskId,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("fileBucket") FileBucket fileBucket,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail)
      throws UnableToRegisterIdempotentOperationException {
    logger.info("Received save artifact request for delegateId : {}, taskId: {}, accountId: {}, fileDetail: {}",
        delegateId, taskId, accountId, fileDetail);

    // TODO: Do more check, so one delegate does not overload system

    IdempotentId idempotentid = new IdempotentId(taskId + ":" + fileDetail.getFileName());

    try (IdempotentLock<String> idempotent = idempotentRegistry.create(idempotentid, ofMinutes(1), ofSeconds(1))) {
      if (idempotent.alreadyExecuted()) {
        return new RestResponse<>(idempotent.getResult());
      }
      FileMetadata fileMetadata = new FileMetadata();
      fileMetadata.setFileName(new File(fileDetail.getFileName()).getName());
      String fileId = fileService.saveFile(fileMetadata,
          new BoundedInputStream(uploadedInputStream, configuration.getFileUploadLimits().getAppContainerLimit()),
          fileBucket);
      logger.info("fileId: {} and fileName {}", fileId, fileMetadata.getFileName());

      idempotent.succeeded(fileId);
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
    logger.debug("entityId: {}, fileBucket: {}, version: {}", entityId, fileBucket, version);
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
      @QueryParam("fileBucket") @NotNull FileBucket fileBucket, @QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("encrypted") boolean encrypted) {
    logger.info("fileId: {}, fileBucket: {}", fileId, fileBucket);
    return output -> {
      if (encrypted) {
        File file = new File(Files.createTempDir(), generateUuid());
        logger.info("Temp file path [{}]", file.getAbsolutePath());
        fileService.download(fileId, file, fileBucket);
        EncryptionUtils.decryptToStream(file, accountId, output);
      } else {
        fileService.downloadToStream(fileId, output, fileBucket);
      }
    };
  }

  @DelegateAuth
  @GET
  @Path("metainfo")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateFile> getFileInfo(@QueryParam("fileId") String fileId,
      @QueryParam("fileBucket") @NotNull FileBucket fileBucket, @QueryParam("accountId") @NotEmpty String accountId) {
    logger.info("fileId: {}, fileBucket: {}", fileId, fileBucket);

    GridFSFile gridFSFile = fileService.getGridFsFile(fileId, fileBucket);

    return new RestResponse<>(aDelegateFile()
                                  .withFileId(fileId)
                                  .withBucket(fileBucket)
                                  .withFileName(gridFSFile.getFilename())
                                  .withLength(gridFSFile.getLength())
                                  .build());
  }
}
