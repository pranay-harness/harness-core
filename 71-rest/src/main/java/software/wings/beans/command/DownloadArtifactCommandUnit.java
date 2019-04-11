package software.wings.beans.command;

import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;

import com.google.common.base.Charsets;
import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.exception.InvalidRequestException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SftpConfig;
import software.wings.beans.SmbConfig;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.common.Constants;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.SftpHelperService;
import software.wings.service.impl.SmbHelperService;
import software.wings.service.intfc.security.EncryptionService;

import java.nio.charset.Charset;
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
  private static final Charset ENCODING = Charsets.UTF_8;

  @Inject private EncryptionService encryptionService;
  @Inject @Transient private transient DelegateLogService delegateLogService;
  @Inject private AwsHelperService awsHelperService;
  @Inject private SmbHelperService smbHelperService;
  @Inject private SftpHelperService sftpHelperService;
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
    if (StringUtils.isEmpty(getCommandPath())) {
      saveExecutionLog(context, ERROR, "Artifact Download Directory cannot be null or empty");
      throw new InvalidRequestException("Artifact Download Directory cannot be null or empty", USER);
    }
    ArtifactStreamType artifactStreamType =
        ArtifactStreamType.valueOf(context.getArtifactStreamAttributes().getArtifactStreamType());
    String command;
    switch (artifactStreamType) {
      case AMAZON_S3:
        command = constructCommandStringForAmazonS3(context);
        logger.info("Downloading artifact from " + artifactStreamType.name() + " to " + getCommandPath());
        saveExecutionLog(
            context, INFO, "Downloading artifact from " + artifactStreamType.name() + " to " + getCommandPath());
        return context.executeCommandString(command, false);
      case ARTIFACTORY:
        command = constructCommandStringForArtifactory(context);
        logger.info("Downloading artifact from " + artifactStreamType.name() + " to " + getCommandPath());
        saveExecutionLog(
            context, INFO, "Downloading artifact from " + artifactStreamType.name() + " to " + getCommandPath());
        return context.executeCommandString(command, false);
      case SMB:
        command = constructCommandStringForSMB(context);
        logger.info("Downloading artifact from " + artifactStreamType.name() + " to " + getCommandPath());
        saveExecutionLog(
            context, INFO, "Downloading artifact from " + artifactStreamType.name() + " to " + getCommandPath());
        return context.executeCommandString(command, false);
      case SFTP:
        command = constructCommandStringForSFTP(context);
        logger.info("Downloading artifact from " + artifactStreamType.name() + " to " + getCommandPath());
        saveExecutionLog(
            context, INFO, "Downloading artifact from " + artifactStreamType.name() + " to " + getCommandPath());
        return context.executeCommandString(command, false);

      default:
        saveExecutionLog(context, ERROR,
            format("Download Artifact not supported for Artifact Stream Type %s", artifactStreamType.name()));
        throw new InvalidRequestException(
            format("Download Artifact not supported for Artifact Stream Type %s", artifactStreamType.name()), USER);
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
    String authorizationHeader = getAmazonS3AuthorizationHeader(context, date);
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
        throw new InvalidRequestException("Invalid Script type", USER);
    }
    return command;
  }

  private String getAmazonS3AuthorizationHeader(ShellCommandExecutionContext context, String date) {
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
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      return new byte[] {};
    }
  }

  private void saveExecutionLog(ShellCommandExecutionContext context, LogLevel logLevel, String line) {
    delegateLogService.save(context.getAccountId(),
        aLog()
            .withAppId(context.getAppId())
            .withActivityId(context.getActivityId())
            .withHostName(context.getHost().getPublicDns())
            .withLogLevel(logLevel)
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

  private String constructCommandStringForSMB(ShellCommandExecutionContext context) {
    Map<String, String> metadata = context.getMetadata();
    String artifactFileName = metadata.get(Constants.ARTIFACT_FILE_NAME);
    String artifactPath = metadata.get(Constants.ARTIFACT_PATH);
    SmbConfig smbConfig = (SmbConfig) context.getArtifactStreamAttributes().getServerSetting().getValue();
    List<EncryptedDataDetail> encryptionDetails = context.getArtifactServerEncryptedDataDetails();
    encryptionService.decrypt(smbConfig, encryptionDetails);
    String command;
    switch (this.getScriptType()) {
      case POWERSHELL:
        String domain = isNotEmpty(smbConfig.getDomain()) ? smbConfig.getDomain() + "\\" : "";
        String userPassword = "/user:" + domain + smbConfig.getUsername() + " " + new String(smbConfig.getPassword());
        String shareUrl = smbHelperService.getSMBConnectionHost(smbConfig.getSmbUrl()) + "\\"
            + smbHelperService.getSharedFolderName(smbConfig.getSmbUrl());

        String artifactFolder = getArtifactFolder(artifactPath, artifactFileName);
        if (!isEmpty(artifactFolder)) {
          shareUrl = shareUrl + "\\" + artifactFolder;
        }

        String roboCopyCommand = "robocopy \\\\" + shareUrl + " " + getCommandPath() + " " + artifactFileName;
        command = "net use \\\\" + shareUrl + " " + userPassword + " /persistent:no\n " + roboCopyCommand;
        break;
      default:
        throw new InvalidRequestException("Invalid Script type", USER);
    }
    return command;
  }

  private static String getArtifactFolder(String artifactPath, String artifactFileName) {
    return artifactPath.substring(0, artifactPath.lastIndexOf(artifactFileName));
  }

  private String constructCommandStringForSFTP(ShellCommandExecutionContext context) {
    Map<String, String> metadata = context.getMetadata();
    String artifactPath = metadata.get(Constants.ARTIFACT_PATH);
    SftpConfig sftpConfig = (SftpConfig) context.getArtifactStreamAttributes().getServerSetting().getValue();
    List<EncryptedDataDetail> encryptionDetails = context.getArtifactServerEncryptedDataDetails();
    encryptionService.decrypt(sftpConfig, encryptionDetails);
    String command;
    switch (this.getScriptType()) {
      case POWERSHELL:
        command = "if (Get-Module -ListAvailable -Name Posh-SSH) {\n"
            + "    Write-Host \"Posh-SSH Module exists\"\n"
            + "\n"
            + "} else {\n"
            + "    Write-Host \"Module does not exist, Installing\"\n"
            + "    Install-PackageProvider -Name NuGet -MinimumVersion 2.8.5.201 -Force\n"
            + "    Install-Module -Name Posh-SSH -SkipPublisherCheck -Force\n"
            + "}\n"
            + "\n"
            + "Import-Module Posh-SSH\n"
            + "\n"
            + "$Password = ConvertTo-SecureString "
            + "\'" + new String(sftpConfig.getPassword()) + "\'"
            + " -AsPlainText -Force\n"
            + "$Credential = New-Object System.Management.Automation.PSCredential ('" + sftpConfig.getUsername() + "\'"
            + ", $Password)\n"
            + "\n"
            + "$RemotePath = \"" + artifactPath + "\"\n"
            + "$LocalPath = " + getCommandPath() + "\n"
            + "\n"
            + "$SftpIp = \'" + sftpHelperService.getSFTPConnectionHost(sftpConfig.getSftpUrl()) + "\'\n"
            + "\n"
            + "$ThisSession = New-SFTPSession -ComputerName $SftpIp -Credential $Credential -AcceptKey\n"
            + "\n"
            + "$sftpPathAttribute = Get-SFTPPathAttribute -SessionId ($ThisSession).SessionId -Path  $Remotepath \n"
            + "if ( $sftpPathAttribute.IsRegularFile ) \n"
            + "{ \n"
            + "Get-SFTPFile -SessionId ($ThisSession).SessionId -RemoteFile $Remotepath -LocalPath $LocalPath -Overwrite\n"
            + "} \n\n"
            + "if ( $sftpPathAttribute.IsDirectory ) \n"
            + "{ \n"
            + "$sublist = New-Object System.Collections.Arraylist \n"
            + "Get-SFTPChildItem -SessionId ($ThisSession).SessionId -Path $Remotepath -Recursive | ForEach-Object {  $sublist.add($_.FullName) } \n"
            + "for($i=0; $i -le $sublist.Count; $i++) { \n"
            + "if ( $sublist[$i] ) {\n"
            + "$sftpPathAttribute = Get-SFTPPathAttribute -SessionId ($ThisSession).SessionId -Path  $sublist[$i] \n"
            + "if ( $sftpPathAttribute.IsRegularFile ) {\n"
            + "$FullLocalPath = $LocalPath + \"\\\" + $Remotepath \n"
            + "New-Item -ItemType Directory -Force -Path $FullLocalPath \n"
            + "Get-SFTPFile -SessionId ($ThisSession).SessionId -RemoteFile $sublist[$i] -LocalPath $FullLocalPath -Overwrite \n"
            + "} \n"
            + "} \n"
            + "} \n"
            + "} \n\n"
            + "\n"
            + "Get-SFTPSession | % { Remove-SFTPSession -SessionId ($_.SessionId) }";
        break;

      default:
        throw new InvalidRequestException("Invalid Script type", USER);
    }
    return command;
  }

  private String constructCommandStringForArtifactory(ShellCommandExecutionContext context) {
    Map<String, String> metadata = context.getMetadata();
    String artifactFileName = metadata.get(Constants.ARTIFACT_FILE_NAME);
    int lastIndexOfSlash = artifactFileName.lastIndexOf('/');
    if (lastIndexOfSlash > 0) {
      artifactFileName = artifactFileName.substring(lastIndexOfSlash + 1);
      logger.info("Got filename: " + artifactFileName);
    }
    ArtifactoryConfig artifactoryConfig =
        (ArtifactoryConfig) context.getArtifactStreamAttributes().getServerSetting().getValue();
    List<EncryptedDataDetail> encryptionDetails = context.getArtifactServerEncryptedDataDetails();
    encryptionService.decrypt(artifactoryConfig, encryptionDetails);
    String authHeader = null;
    if (artifactoryConfig.hasCredentials()) {
      String pair = artifactoryConfig.getUsername() + ":" + new String(artifactoryConfig.getPassword());
      authHeader = "Basic " + encodeBase64(pair);
    }
    String command;
    switch (this.getScriptType()) {
      case BASH:
        if (!artifactoryConfig.hasCredentials()) {
          command = "curl --progress-bar -X GET \""
              + getArtifactoryUrl(artifactoryConfig, metadata.get(Constants.ARTIFACT_PATH)) + "\" -o \""
              + getCommandPath() + "/" + artifactFileName + "\"";
        } else {
          command = "curl --progress-bar -H \"Authorization: " + authHeader + "\" -X GET \""
              + getArtifactoryUrl(artifactoryConfig, metadata.get(Constants.ARTIFACT_PATH)) + "\" -o \""
              + getCommandPath() + "/" + artifactFileName + "\"";
        }
        break;
      case POWERSHELL:
        if (!artifactoryConfig.hasCredentials()) {
          command =
              "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12\n Invoke-WebRequest -Uri \""
              + getArtifactoryUrl(artifactoryConfig, metadata.get(Constants.ARTIFACT_PATH)) + "\" -OutFile \""
              + getCommandPath() + "\\" + artifactFileName + "\"";
        } else {
          command = "$Headers = @{\n"
              + "    Authorization = \"" + authHeader + "\"\n"
              + "}\n [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12"
              + "\n Invoke-WebRequest -Uri \""
              + getArtifactoryUrl(artifactoryConfig, metadata.get(Constants.ARTIFACT_PATH))
              + "\" -Headers $Headers -OutFile \"" + getCommandPath() + "\\" + artifactFileName + "\"";
        }
        break;
      default:
        throw new InvalidRequestException("Invalid Script type", USER);
    }
    return command;
  }

  private String getArtifactoryUrl(ArtifactoryConfig config, String artifactPath) {
    return config.fetchRegistryUrl() + "/" + artifactPath;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String commandPath;
    private ScriptType scriptType;
    private String name;
    private CommandUnitType commandUnitType;

    private Builder() {}

    /**
     * A DownloadArtifact command unit builder.
     *
     * @return the builder
     */
    public static Builder aDownloadArtifactCommandUnit() {
      return new Builder();
    }

    /**
     * With file category builder.
     *
     * @param commandPath the default command directory
     * @return the builder
     */
    public Builder withCommandPath(String commandPath) {
      this.commandPath = commandPath;
      return this;
    }

    /**
     * With destination directory path builder.
     *
     * @param scriptType the script type
     * @return the builder
     */
    public Builder withScriptType(ScriptType scriptType) {
      this.scriptType = scriptType;
      return this;
    }

    /**
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With command unit type builder.
     *
     * @param commandUnitType the command unit type
     * @return the builder
     */
    public Builder withCommandUnitType(CommandUnitType commandUnitType) {
      this.commandUnitType = commandUnitType;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aDownloadArtifactCommandUnit()
          .withCommandPath(commandPath)
          .withScriptType(scriptType)
          .withName(name)
          .withCommandUnitType(commandUnitType);
    }

    /**
     * Build scp command unit.
     *
     * @return the scp command unit
     */
    public DownloadArtifactCommandUnit build() {
      DownloadArtifactCommandUnit downloadArtifactCommandUnit = new DownloadArtifactCommandUnit();
      downloadArtifactCommandUnit.setScriptType(scriptType);
      downloadArtifactCommandUnit.setCommandPath(commandPath);
      downloadArtifactCommandUnit.setName(name);
      downloadArtifactCommandUnit.setCommandUnitType(commandUnitType);
      return downloadArtifactCommandUnit;
    }
  }
}
