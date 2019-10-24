package software.wings.service.impl.aws.model.embed;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import com.google.common.collect.ImmutableMap;

import com.amazonaws.services.lambda.model.AliasConfiguration;
import com.amazonaws.services.lambda.model.FunctionConfiguration;
import com.amazonaws.services.lambda.model.GetFunctionResult;
import com.amazonaws.services.lambda.model.ListAliasesResult;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.logging.log4j.util.Strings;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Builder
@Slf4j
public class AwsLambdaDetails {
  private String functionName;
  private String functionArn;
  private String runtime;
  private String role;
  private String handler;
  private Long codeSize;
  private String description;
  private Integer timeout;
  private Integer memorySize;
  private Date lastModified;
  private String codeSha256;
  private String version;
  private String kMSKeyArn;
  private String masterArn;
  private String revisionId;
  private Map<String, String> tags;
  private List<String> aliases;

  public static AwsLambdaDetails from(GetFunctionResult result, ListAliasesResult listAliasesResult) {
    final FunctionConfiguration config = result.getConfiguration();
    final AwsLambdaDetailsBuilder builder = AwsLambdaDetails.builder()
                                                .functionArn(config.getFunctionArn())
                                                .functionName(config.getFunctionName())
                                                .runtime(config.getRuntime())
                                                .role(config.getRole())
                                                .handler(config.getHandler())
                                                .codeSize(config.getCodeSize())
                                                .description(config.getDescription())
                                                .timeout(config.getTimeout())
                                                .memorySize(config.getMemorySize())
                                                .codeSha256(config.getCodeSha256())
                                                .version(config.getVersion())
                                                .kMSKeyArn(config.getKMSKeyArn())
                                                .masterArn(config.getMasterArn())
                                                .revisionId(config.getRevisionId());

    if (Strings.isNotEmpty(config.getLastModified())) {
      try {
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        builder.lastModified(simpleDateFormat.parse(config.getLastModified()));
      } catch (ParseException e) {
        logger.warn("Unable to parse date [{}]", config.getLastModified());
      }
    }

    if (MapUtils.isNotEmpty(result.getTags())) {
      builder.tags(ImmutableMap.copyOf(result.getTags()));
    }

    if (listAliasesResult != null) {
      builder.aliases(
          emptyIfNull(listAliasesResult.getAliases()).stream().map(AliasConfiguration::getName).collect(toList()));
    }
    return builder.build();
  }
}
