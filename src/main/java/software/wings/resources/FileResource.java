package software.wings.resources;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.wings.app.WingsBootstrap;
import software.wings.beans.ChecksumType;
import software.wings.beans.FileMetadata;
import software.wings.beans.RestResponse;
import software.wings.service.intfc.FileService;

public class FileResource {
  private static final Logger logger = LoggerFactory.getLogger(FileResource.class);

  private FileService fileService;

  public FileResource() {
    fileService = WingsBootstrap.lookup(FileService.class);
  }
  public FileResource(FileService fileService) {
    this.fileService = fileService;
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public RestResponse<String> upload(@QueryParam("md5") String md5, @QueryParam("fileDataType") String fileDataType,
      @QueryParam("fileDataRefId") String fileDataRefId, @FormDataParam("part") InputStream uploadedInputStream,
      @FormDataParam("part") FormDataContentDisposition fileDetail) {
    String filename;
    if (fileDetail == null || fileDetail.getFileName() == null) {
      filename = "file-" + System.currentTimeMillis();
    } else {
      filename = fileDetail.getFileName();
    }

    FileMetadata fileMetadata = new FileMetadata();
    fileMetadata.setFileDataType(fileDataType);
    fileMetadata.setFileRefId(fileDataRefId);
    fileMetadata.setFileName(filename);
    if (StringUtils.isNotBlank(md5)) {
      fileMetadata.setChecksumType(ChecksumType.MD5);
      fileMetadata.setChecksum(md5);
    }
    String fileId = fileService.saveFile(fileMetadata, uploadedInputStream);
    return new RestResponse<String>(fileId);
  }
}
