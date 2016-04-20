package software.wings.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import software.wings.beans.ConfigFile;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.service.intfc.ServiceResourceService;

import javax.ws.rs.*;
import java.io.InputStream;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
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
    ConfigFile configFile = new ConfigFile(serviceID, fileName, relativePath, md5);
    String fileId = srs.saveFile(configFile, uploadedInputStream, CONFIGS);
    return new RestResponse<>(fileId);
  }

  @GET
  @Path("{serviceID}/configs")
  public RestResponse<List<ConfigFile>> fetchConfigs(@PathParam("serviceID") String serviceID) {
    return new RestResponse<>(srs.getConfigs(serviceID));
  }

  @GET
  @Path("{serviceID}/configs/{configID}")
  public RestResponse<ConfigFile> fetchConfig(@PathParam("configID") String configID) {
    return new RestResponse<>(srs.getConfig(configID));
  }

  @PUT
  @Path("{serviceID}/configs/{configID}")
  @Consumes(MULTIPART_FORM_DATA)
  public void updateConfig(@PathParam("serviceID") String serviceID, @PathParam("configID") String configID,
      @FormDataParam("fileName") String fileName, @FormDataParam("relativePath") String relativePath,
      @FormDataParam("md5") String md5, @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    ConfigFile configFile = new ConfigFile(serviceID, fileName, relativePath, md5);
    configFile.setUuid(configID);
    srs.updateFile(configFile, uploadedInputStream, CONFIGS);
  }
}
