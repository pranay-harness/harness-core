package software.wings.beans.command;

import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.common.Constants;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.security.EncryptionService;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@JsonTypeName("DOWNLOAD_ARTIFACT")
@EqualsAndHashCode(callSuper = true)
public class DownloadArtifactCommandUnit extends ExecCommandUnit {
  private static final Logger logger = LoggerFactory.getLogger(DownloadArtifactCommandUnit.class);
  private static final String ALGORITHM = "HmacSHA1";
  private static final String ENCODING = "UTF8";

  @Inject private EncryptionService encryptionService;
  @Inject @Transient private transient DelegateLogService delegateLogService;
  @Inject private AwsHelperService awsHelperService;
  private static Map<String, String> bucketRegions = new HashMap<>();

  /**
   * Instantiates a new Download Artifact command unit.
   */
  public DownloadArtifactCommandUnit() {
    setCommandUnitType(CommandUnitType.DOWNLOAD_ARTIFACT);
    initBucketRegions();
  }

  @Override
  @SchemaIgnore
  public boolean isArtifactNeeded() {
    return true;
  }

  @Override
  protected CommandExecutionStatus executeInternal(ShellCommandExecutionContext context) {
    ArtifactStreamType artifactStreamType =
        ArtifactStreamType.valueOf(context.getArtifactStreamAttributes().getArtifactStreamType());
    String command;
    switch (artifactStreamType) {
      case AMAZON_S3:
        command = constructCommandStringForAmazonS3(context);
        logger.info("Downloading artifact from " + artifactStreamType.name() + " to " + getCommandPath());
        saveExecutionLog(context, "Downloading artifact from " + artifactStreamType.name() + " to " + getCommandPath());
        return context.executeCommandString(command, false);
      default:
        throw new WingsException(ErrorCode.UNKNOWN_ARTIFACT_TYPE);
    }
  }

  @Attributes(title = "Command")
  @Override
  public String getCommandString() {
    return null;
  }

  @SchemaIgnore
  @Override
  public String getCommandPath() {
    return super.getCommandPath();
  }

  @SchemaIgnore
  @Override
  public String getPreparedCommand() {
    return super.getPreparedCommand();
  }

  @SchemaIgnore
  @Override
  public List<TailFilePatternEntry> getTailPatterns() {
    return super.getTailPatterns();
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("DOWNLOAD_ARTIFACT")
  public static class Yaml extends ExecCommandUnit.AbstractYaml {
    public Yaml() {
      super(CommandUnitType.DOWNLOAD_ARTIFACT.name());
    }

    @lombok.Builder
    public Yaml(String name, String deploymentType, String workingDirectory, String scriptType, String command,
        List<TailFilePatternEntry.Yaml> filePatternEntryList) {
      super(name, CommandUnitType.DOWNLOAD_ARTIFACT.name(), deploymentType, workingDirectory, scriptType, command,
          filePatternEntryList);
    }
  }

  private String constructCommandStringForAmazonS3(ShellCommandExecutionContext context) {
    Map<String, String> metadata = context.getMetadata();
    String date = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME);
    String bucketName = metadata.get(Constants.BUCKET_NAME);
    String artifactPath = metadata.get(Constants.ARTIFACT_PATH);
    String artifactFileName = metadata.get(Constants.ARTIFACT_FILE_NAME);
    int lastIndexOfSlash = artifactFileName.lastIndexOf('/');
    if (lastIndexOfSlash > 0) {
      artifactFileName = artifactFileName.substring(lastIndexOfSlash + 1);
      logger.info("Got filename: " + artifactFileName);
    }
    AwsConfig awsConfig = (AwsConfig) context.getArtifactStreamAttributes().getServerSetting().getValue();
    List<EncryptedDataDetail> encryptionDetails = context.getArtifactServerEncryptedDataDetails();
    String region = awsHelperService.getBucketRegion(awsConfig, encryptionDetails, bucketName);
    String hostName = getAmazonS3HostName(bucketName);
    String url = getAmazonS3Url(bucketName, region, artifactPath);
    String authorizationHeader = getAuthorizationHeader(context, date);
    String command;
    switch (this.getScriptType()) {
      case POWERSHELL:
        command = "$Headers = @{\n"
            + "    Authorization = \"" + authorizationHeader + "\"\n"
            + "    Date = \"" + date + "\"\n"
            + "    Host = \"" + hostName + "\"\n"
            + "}\n Invoke-WebRequest -Uri \"" + url + "\" -Headers $Headers -OutFile \"" + getCommandPath() + "\\"
            + artifactFileName + "\"";
        break;
      case BASH:
        command = "curl --progress-bar \"" + url + "\" -H \"Host: " + hostName + "\" \\\n"
            + "-H \"Date: " + date + "\" \\\n"
            + "-H \"Authorization: " + authorizationHeader + "\" -o \"" + getCommandPath() + "/" + artifactFileName
            + "\"";
        break;
      default:
        throw new WingsException("Invalid Script type");
    }
    return command;
  }

  private String getAuthorizationHeader(ShellCommandExecutionContext context, String date) {
    AwsConfig awsConfig = (AwsConfig) context.getArtifactStreamAttributes().getServerSetting().getValue();
    List<EncryptedDataDetail> encryptionDetails = context.getArtifactServerEncryptedDataDetails();
    encryptionService.decrypt(awsConfig, encryptionDetails);
    Map<String, String> metadata = context.getMetadata();
    String AWSAccessKeyId = awsConfig.getAccessKey();
    String AWSSecretAccessKey = String.valueOf(awsConfig.getSecretKey());

    String canonicalizedResource =
        "/" + metadata.get(Constants.BUCKET_NAME) + "/" + metadata.get(Constants.ARTIFACT_FILE_NAME);
    String stringToSign = "GET\n\n\n" + date + "\n" + canonicalizedResource;
    String signature = Base64.getEncoder().encodeToString(hmacSHA1(stringToSign, AWSSecretAccessKey));
    return "AWS"
        + " " + AWSAccessKeyId + ":" + signature;
  }

  private byte[] hmacSHA1(String data, String key) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(key.getBytes(ENCODING), ALGORITHM));
      return mac.doFinal(data.getBytes(ENCODING));
    } catch (NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException e) {
      return new byte[] {};
    }
  }

  private void saveExecutionLog(ShellCommandExecutionContext context, String line) {
    delegateLogService.save(context.getAccountId(),
        aLog()
            .withAppId(context.getAppId())
            .withActivityId(context.getActivityId())
            .withHostName(context.getHost().getPublicDns())
            .withLogLevel(INFO)
            .withCommandUnitName(getName())
            .withLogLine(line)
            .withExecutionResult(RUNNING)
            .build());
  }

  private String getAmazonS3HostName(String bucketName) {
    return bucketName + ".s3.amazonaws.com";
  }

  private String getAmazonS3Url(String bucketName, String region, String artifactPath) {
    return "https://" + bucketName + ".s3" + bucketRegions.get(region) + ".amazonaws.com"
        + "/" + artifactPath;
  }

  private void initBucketRegions() {
    bucketRegions.put("us-east-2", "-us-east-2");
    bucketRegions.put("us-east-1", "");
    bucketRegions.put("us-west-1", "-us-west-1");
    bucketRegions.put("us-west-2", "-us-west-2");
    bucketRegions.put("ca-central-1", "-ca-central-1");
    bucketRegions.put("ap-south-1", "-ap-south-1");
    bucketRegions.put("ap-northeast-2", "-ap-northeast-2");
    bucketRegions.put("ap-northeast-3", "-ap-northeast-3");
    bucketRegions.put("ap-southeast-1", "-ap-southeast-1");
    bucketRegions.put("ap-northeast-1", "-ap-northeast-1");
    bucketRegions.put("cn-north-1", ".cn-north-1");
    bucketRegions.put("cn-northwest-1", ".cn-northwest-1");
    bucketRegions.put("eu-central-1", "-eu-central-1");
    bucketRegions.put("eu-west-1", "-eu-west-1");
    bucketRegions.put("eu-west-2", "-eu-west-2");
    bucketRegions.put("eu-west-3", "-eu-west-3");
    bucketRegions.put("sa-east-1", "-sa-east-1");
  }
}