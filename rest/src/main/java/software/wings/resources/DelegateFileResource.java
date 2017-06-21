package software.wings.resources;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.delegatetasks.DelegateFile.Builder.aDelegateFile;
import static software.wings.service.intfc.FileService.FileBucket.ARTIFACTS;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.mongodb.client.gridfs.model.GridFSFile;
import io.swagger.annotations.Api;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.FileMetadata;
import software.wings.beans.RestResponse;
import software.wings.delegatetasks.DelegateFile;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.DelegateAuth;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.utils.BoundedInputStream;

import java.io.File;
import java.io.InputStream;
import javax.inject.Inject;
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
@AuthRule(ResourceType.DELEGATE)
public class DelegateFileResource {
  private final Logger logger = LoggerFactory.getLogger(DelegateFileResource.class);

  private FileService fileService;
  private MainConfiguration configuration;

  @Inject
  public DelegateFileResource(FileService fileService, MainConfiguration configuration) {
    this.fileService = fileService;
    this.configuration = configuration;
  }

  @DelegateAuth
  @POST
  @Path("{delegateId}/tasks/{taskId}")
  @Consumes(MULTIPART_FORM_DATA)
  @Timed
  @ExceptionMetered
  public RestResponse<String> saveArtifact(@PathParam("delegateId") String delegateId,
      @PathParam("taskId") String taskId, @QueryParam("accountId") @NotEmpty String accountId,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    logger.debug(
        "delegateId : {}, taskId: {}, accountId: {}, fileDetail: {}", delegateId, taskId, accountId, fileDetail);

    // TODO: Do more check, so one delegate does not overload system

    FileMetadata fileMetadata = new FileMetadata();
    fileMetadata.setFileName(new File(fileDetail.getFileName()).getName());
    String fileId = fileService.saveFile(fileMetadata,
        new BoundedInputStream(uploadedInputStream, configuration.getFileUploadLimits().getConfigFileLimit()),
        ARTIFACTS);
    logger.debug("fileId: {}", fileId);
    return new RestResponse<>(fileId);
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
  @Path("download")
  @Timed
  @ExceptionMetered
  public StreamingOutput downloadFile(@QueryParam("fileId") @NotEmpty String fileId,
      @QueryParam("fileBucket") @NotNull FileBucket fileBucket, @QueryParam("accountId") @NotEmpty String accountId) {
    logger.debug("fileId: {}, fileBucket: {}", fileId, fileBucket);

    return output -> {
      fileService.downloadToStream(fileId, output, fileBucket);
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
