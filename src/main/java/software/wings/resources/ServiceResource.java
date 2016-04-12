package software.wings.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.mongodb.DBObject;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import software.wings.beans.*;
import software.wings.service.intfc.ServiceResourceService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.beans.ChecksumType.MD5;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;

/**
 * Created by anubhaw on 3/25/16.
 */

@Path("/services")
@Timed
@ExceptionMetered
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class ServiceResource {
  @Inject private ServiceResourceService srs;

  @GET
  @Path("{appID}")
  public RestResponse<List<Service>> list(@PathParam("appID") String appID) {
    return new RestResponse<>(srs.list(appID));
  }

  @POST
  @Path("{appID}")
  public RestResponse<Service> save(@PathParam("appID") String appID, Service service) {
    return new RestResponse<>(srs.save(appID, service));
  }

  @PUT
  @Path("{appID}")
  public RestResponse<Service> update(Service service) {
    return new RestResponse<>(srs.update(service));
  }

  @POST
  @Path("{serviceID}/configs")
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse<String> uploadConfig(@PathParam("serviceID") String serviceID,
      @FormDataParam("fileName") String fileName, @FormDataParam("relativePath") String relativePath,
      @FormDataParam("md5") String md5, @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    FileMetadata fileMetadata = new FileMetadata();
    fileMetadata.setFileName(fileName);
    if (StringUtils.isNotBlank(md5)) {
      fileMetadata.setChecksumType(MD5);
      fileMetadata.setChecksum(md5);
    }
    fileMetadata.setRelativePath(relativePath);
    String fileId = srs.saveFile(serviceID, fileMetadata, uploadedInputStream, CONFIGS);
    return new RestResponse<>(fileId);
  }

  @GET
  @Path("{serviceID}/configs")
  public RestResponse<List<DBObject>> fetchConfigs(@PathParam("serviceID") String serviceID) {
    return new RestResponse<>(srs.fetchConfigs(serviceID));
  }
}
