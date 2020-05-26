package software.wings.graphql.datafetcher.cloudProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import org.apache.commons.lang3.ObjectUtils;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsConfig.AwsConfigBuilder;
import software.wings.beans.AwsCrossAccountAttributes;
import software.wings.beans.AwsCrossAccountAttributes.AwsCrossAccountAttributesBuilder;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.aws.QLAwsCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.aws.QLAwsCrossAccountAttributes;
import software.wings.graphql.schema.mutation.cloudProvider.aws.QLAwsManualCredentials;
import software.wings.graphql.schema.type.secrets.QLUsageScope;

@Singleton
public class AwsDataFetcherHelper {
  @Inject private UsageScopeController usageScopeController;

  public SettingAttribute toSettingAttribute(QLAwsCloudProviderInput input, String accountId) {
    AwsConfigBuilder configBuilder = AwsConfig.builder().accountId(accountId);

    if (input.getUseEc2IamCredentials().isPresent()) {
      input.getUseEc2IamCredentials().getValue().ifPresent(useEc2 -> {
        configBuilder.useEc2IamCredentials(useEc2);
        if (useEc2) {
          configBuilder.tag(
              input.getEc2IamCredentials()
                  .getValue()
                  .orElseThrow(() -> new InvalidRequestException("No ec2IamCredentials provided with the request."))
                  .getTag()
                  .getValue()
                  .orElseThrow(() -> new InvalidRequestException("No delegate tag provided with the request.")));
        } else {
          QLAwsManualCredentials credentials = input.getManualCredentials().getValue().orElseThrow(
              () -> new InvalidRequestException("No manualCredentials provided with the request."));

          configBuilder.accessKey(credentials.getAccessKey().getValue().orElseThrow(
              () -> new InvalidRequestException("No accessKey provided with the request.")));
          configBuilder.encryptedSecretKey(credentials.getSecretKeySecretId().getValue().orElseThrow(
              () -> new InvalidRequestException("No secretKeySecretId provided with the request.")));
        }
      });
    } else {
      throw new InvalidRequestException("No useEc2IamCredentials provided with the request.");
    }

    if (input.getAssumeCrossAccountRole().isPresent()) {
      input.getAssumeCrossAccountRole().getValue().ifPresent(assumeCrossAccountRole -> {
        configBuilder.assumeCrossAccountRole(assumeCrossAccountRole);

        if (assumeCrossAccountRole) {
          AwsCrossAccountAttributesBuilder builder = AwsCrossAccountAttributes.builder();

          QLAwsCrossAccountAttributes crossAccountAttributes = input.getCrossAccountAttributes().getValue().orElseThrow(
              () -> new InvalidRequestException("No crossAccountAttributes provided with the request."));

          builder.crossAccountRoleArn(crossAccountAttributes.getCrossAccountRoleArn().getValue().orElseThrow(
              () -> new InvalidRequestException("No crossAccountRoleArn provided with the request.")));

          builder.externalId(crossAccountAttributes.getExternalId().getValue().orElseThrow(
              () -> new InvalidRequestException("No externalId provided with the request.")));

          configBuilder.crossAccountAttributes(builder.build());
        }
      });
    }

    SettingAttribute.Builder settingAttributeBuilder = SettingAttribute.Builder.aSettingAttribute()
                                                           .withValue(configBuilder.build())
                                                           .withAccountId(accountId)
                                                           .withCategory(SettingAttribute.SettingCategory.SETTING);

    if (input.getName().isPresent()) {
      input.getName().getValue().ifPresent(settingAttributeBuilder::withName);
    }

    if (input.getUsageScope().isPresent()) {
      settingAttributeBuilder.withUsageRestrictions(
          usageScopeController.populateUsageRestrictions(input.getUsageScope().getValue().orElse(null), accountId));
    }

    return settingAttributeBuilder.build();
  }

  public void updateSettingAttribute(
      SettingAttribute settingAttribute, QLAwsCloudProviderInput input, String accountId) {
    AwsConfig config = (AwsConfig) settingAttribute.getValue();

    if (input.getUseEc2IamCredentials().isPresent()) {
      input.getUseEc2IamCredentials().getValue().ifPresent(useEc2 -> {
        config.setUseEc2IamCredentials(useEc2);
        if (useEc2) {
          input.getEc2IamCredentials()
              .getValue()
              .flatMap(credentials -> credentials.getTag().getValue())
              .ifPresent(config::setTag);
        } else {
          input.getManualCredentials().getValue().ifPresent(credentials -> {
            credentials.getAccessKey().getValue().ifPresent(config::setAccessKey);
            credentials.getSecretKeySecretId().getValue().ifPresent(config::setEncryptedSecretKey);
          });
        }
      });
    }

    if (input.getAssumeCrossAccountRole().isPresent()) {
      input.getAssumeCrossAccountRole().getValue().ifPresent(assumeCrossAccountRole -> {
        config.setAssumeCrossAccountRole(assumeCrossAccountRole);

        if (assumeCrossAccountRole) {
          AwsCrossAccountAttributes awsCrossAccountAttributes = ObjectUtils.defaultIfNull(
              config.getCrossAccountAttributes(), AwsCrossAccountAttributes.builder().build());

          input.getCrossAccountAttributes().getValue().ifPresent(crossAccountAttributes -> {
            crossAccountAttributes.getCrossAccountRoleArn().getValue().ifPresent(
                awsCrossAccountAttributes::setCrossAccountRoleArn);
            crossAccountAttributes.getExternalId().getValue().ifPresent(awsCrossAccountAttributes::setExternalId);
          });

          config.setCrossAccountAttributes(awsCrossAccountAttributes);
        }
      });
    }

    settingAttribute.setValue(config);

    if (input.getName().isPresent()) {
      input.getName().getValue().ifPresent(settingAttribute::setName);
    }

    if (input.getUsageScope().isPresent()) {
      QLUsageScope usageScope = input.getUsageScope().getValue().orElse(null);
      settingAttribute.setUsageRestrictions(usageScopeController.populateUsageRestrictions(usageScope, accountId));
    }
  }
}
