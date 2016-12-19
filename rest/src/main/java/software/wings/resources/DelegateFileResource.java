package software.wings.resources;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.service.intfc.FileService.FileBucket.ARTIFACTS;

import io.swagger.annotations.Api;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.FileMetadata;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.DelegateAuth;
import software.wings.service.intfc.FileService;
import software.wings.utils.BoundedInputStream;

import java.io.InputStream;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rishi on 12/19/16.
 */
@Api("delegates")
@Path("/delegateFiles")
@Produces("application/json")
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
  public RestResponse<String> saveArtifact(@PathParam("delegateId") String delegateId,
      @PathParam("taskId") String taskId, @QueryParam("accountId") @NotEmpty String accountId,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    logger.debug(
        "delegateId : {}, taskId: {}, accountId: {}, fileDetail: {}", delegateId, taskId, accountId, fileDetail);

    // TODO: Do more check, so one delegate does not overload system

    FileMetadata fileMetadata = new FileMetadata();
    fileMetadata.setFileName(fileDetail.getFileName());
    String fileId = fileService.saveFile(fileMetadata,
        new BoundedInputStream(uploadedInputStream, configuration.getFileUploadLimits().getConfigFileLimit()),
        ARTIFACTS);
    logger.debug("fileId: {}", fileId);
    return new RestResponse<>(fileId);
  }
}
