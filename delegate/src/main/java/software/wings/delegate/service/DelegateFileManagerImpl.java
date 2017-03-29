package software.wings.delegate.service;

import static java.lang.System.currentTimeMillis;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.managerclient.SafeHttpCall.execute;

import com.google.inject.Singleton;

import okhttp3.MediaType;
import okhttp3.MultipartBody.Part;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.apache.commons.io.IOUtils;
import retrofit2.Response;
import software.wings.beans.RestResponse;
import software.wings.delegate.app.DelegateConfiguration;
import software.wings.delegatetasks.DelegateFile;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.managerclient.ManagerClient;
import software.wings.service.intfc.FileService.FileBucket;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.inject.Inject;

/**
 * Created by rishi on 12/19/16.
 */
@Singleton
public class DelegateFileManagerImpl implements DelegateFileManager {
  @Inject private ManagerClient managerClient;

  @Inject private DelegateConfiguration delegateConfiguration;

  @Override
  public DelegateFile upload(DelegateFile delegateFile, File content) throws IOException {
    RequestBody filename = RequestBody.create(MediaType.parse(MULTIPART_FORM_DATA), "file");

    // createStateMachine RequestBody instance from file
    RequestBody requestFile = RequestBody.create(MediaType.parse(MULTIPART_FORM_DATA), content);

    // MultipartBody.Part is used to send also the actual file name
    Part part = Part.createFormData("file", delegateFile.getFileName(), requestFile);

    Response<RestResponse<String>> response =
        managerClient
            .uploadFile(delegateFile.getDelegateId(), delegateFile.getTaskId(), delegateFile.getAccountId(), part)
            .execute();
    delegateFile.setFileId(response.body().getResource());
    return delegateFile;
  }

  @Override
  public DelegateFile upload(DelegateFile delegateFile, InputStream contentSource) throws IOException {
    File file = new File(delegateConfiguration.getLocalDiskPath(), String.valueOf(currentTimeMillis()));
    FileOutputStream fout = new FileOutputStream(file);
    IOUtils.copy(contentSource, fout);
    fout.close();

    return upload(delegateFile, file);
  }

  @Override
  public String getFileIdByVersion(FileBucket fileBucket, String entityId, int version, String accountId)
      throws IOException {
    return execute(managerClient.getFileIdByVersion(entityId, fileBucket, version, accountId)).getResource();
  }

  @Override
  public InputStream downloadByFileId(FileBucket bucket, String fileId, String accountId) throws IOException {
    Response<ResponseBody> response = null;
    try {
      response = managerClient.downloadFile(fileId, bucket, accountId).execute();
      return response.body().byteStream();
    } finally {
      if (response != null && !response.isSuccessful()) {
        response.errorBody().close();
      }
    }
  }

  @Override
  public DelegateFile getMetaInfo(FileBucket fileBucket, String fileId, String accountId) throws IOException {
    return execute(managerClient.getMetaInfo(fileId, fileBucket, accountId)).getResource();
  }
}
