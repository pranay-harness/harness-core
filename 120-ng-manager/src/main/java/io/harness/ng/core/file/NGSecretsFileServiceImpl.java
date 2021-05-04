package io.harness.ng.core.file;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.security.SimpleEncryption.CHARSET;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.secrets.SecretsFileService;

import software.wings.app.FileUploadLimit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.gridfs.model.GridFSFile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.CharBuffer;
import javax.validation.executable.ValidateOnExecution;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

@ValidateOnExecution
@Singleton
@OwnedBy(PL)
public class NGSecretsFileServiceImpl implements SecretsFileService {
  private final FileUploadLimit fileUploadLimit;
  private final GridFsTemplate gridFsTemplate;
  private final GridFsOperations gridFsOperations;

  @Inject
  public NGSecretsFileServiceImpl(
      FileUploadLimit fileUploadLimit, GridFsTemplate gridFsTemplate, GridFsOperations gridFsOperations) {
    this.fileUploadLimit = fileUploadLimit;
    this.gridFsTemplate = gridFsTemplate;
    this.gridFsOperations = gridFsOperations;
  }

  @Override
  public String createFile(String name, String accountId, char[] fileContent) {
    DBObject metaData = new BasicDBObject();
    metaData.put("type", "secretFile");
    metaData.put("accountIdentifier", accountId);
    ObjectId id = gridFsTemplate.store(
        new ByteArrayInputStream(CHARSET.encode(CharBuffer.wrap(fileContent)).array()), name, metaData);
    return id.toString();
  }

  @Override
  public char[] getFileContents(String fileId) {
    GridFSFile gridFSFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(String.valueOf(fileId))));
    if (gridFSFile == null) {
      throw new InvalidRequestException("No secret file found with the given id");
    }
    try {
      InputStream inputStream = gridFsOperations.getResource(gridFSFile).getInputStream();
      return IOUtils.toCharArray(inputStream);
    } catch (IOException exception) {
      throw new UnexpectedException("IOException occurred while fetching secret file content");
    }
  }

  @Override
  public long getFileSizeLimit() {
    return this.fileUploadLimit.getEncryptedFileLimit();
  }

  @Override
  public void deleteFile(char[] fileId) {
    gridFsTemplate.delete(new Query(Criteria.where("_id").is(String.valueOf(fileId))));
  }
}
