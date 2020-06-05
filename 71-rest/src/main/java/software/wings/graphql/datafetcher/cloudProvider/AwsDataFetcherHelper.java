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
import software.wings.graphql.schema.mutation.cloudProvider.aws.QLAwsManualCredentials;
import software.wings.graphql.schema.mutation.cloudProvider.aws.QLEc2IamCredentials;
import software.wings.graphql.schema.mutation.cloudProvider.aws.QLUpdateAwsCloudProviderInput;
import software.wings.graphql.schema.type.secrets.QLUsageScope;

@Singleton
public class AwsDataFetcherHelper {
  @Inject private UsageScopeController usageScopeController;

  public SettingAttribute toSettingAttribute(QLAwsCloudProviderInput input, String accountId) {
    AwsConfigBuilder configBuilder = AwsConfig.builder().accountId(accountId);

    if (input.getCredentialsType().isPresent() && input.getCredentialsType().getValue().isPresent()) {
      input.getCredentialsType().getValue().ifPresent(credentialsType -> {
        switch (credentialsType) {
          case EC2_IAM: {
            QLEc2IamCredentials credentials = input.getEc2IamCredentials().getValue().orElseThrow(
                () -> new InvalidRequestException("No ec2IamCredentials provided with the request."));

            configBuilder.useEc2IamCredentials(true);
            configBuilder.tag(credentials.getDelegateSelector().getValue().orElseThrow(
                () -> new InvalidRequestException("No delegateSelector provided with the request.")));
          } break;
          case MANUAL: {
            QLAwsManualCredentials credentials = input.getManualCredentials().getValue().orElseThrow(
                () -> new InvalidRequestException("No manualCredentials provided with the request."));

            configBuilder.useEc2IamCredentials(false);
            configBuilder.accessKey(credentials.getAccessKey().getValue().orElseThrow(
                () -> new InvalidRequestException("No accessKey provided with the request.")));
            configBuilder.encryptedSecretKey(credentials.getSecretKeySecretId().getValue().orElseThrow(
                () -> new InvalidRequestException("No secretKeySecretId provided with the request.")));
          } break;
          default:
            throw new InvalidRequestException("Invalid credentials type");
        }
      });
    } else {
      throw new InvalidRequestException("No credentialsType provided with the request.");
    }

    if (input.getCrossAccountAttributes().isPresent()) {
      input.getCrossAccountAttributes().getValue().ifPresent(crossAccountAttributes -> {
        configBuilder.assumeCrossAccountRole(crossAccountAttributes.getAssumeCrossAccountRole().getValue().orElseThrow(
            () -> new InvalidRequestException("No assumeCrossAccountRole provided with the request.")));

        AwsCrossAccountAttributesBuilder builder = AwsCrossAccountAttributes.builder();

        builder.crossAccountRoleArn(crossAccountAttributes.getCrossAccountRoleArn().getValue().orElseThrow(
            () -> new InvalidRequestException("No crossAccountRoleArn provided with the request.")));

        builder.externalId(crossAccountAttributes.getExternalId().getValue().orElseThrow(
            () -> new InvalidRequestException("No externalId provided with the request.")));

        configBuilder.crossAccountAttributes(builder.build());
      });
    }

    SettingAttribute.Builder settingAttributeBuilder = SettingAttribute.Builder.aSettingAttribute()
                                                           .withValue(configBuilder.build())
                                                           .withAccountId(accountId)
                                                           .withCategory(SettingAttribute.SettingCategory.SETTING);

    if (input.getName().isPresent() && input.getName().getValue().isPresent()) {
      input.getName().getValue().ifPresent(settingAttributeBuilder::withName);
    } else {
      throw new InvalidRequestException("No name provided with the request.");
    }

    if (input.getUsageScope().isPresent()) {
      settingAttributeBuilder.withUsageRestrictions(
          usageScopeController.populateUsageRestrictions(input.getUsageScope().getValue().orElse(null), accountId));
    }

    return settingAttributeBuilder.build();
  }

  public void updateSettingAttribute(
      SettingAttribute settingAttribute, QLUpdateAwsCloudProviderInput input, String accountId) {
    AwsConfig config = (AwsConfig) settingAttribute.getValue();

    if (input.getCredentialsType().isPresent() && input.getCredentialsType().getValue().isPresent()) {
      input.getCredentialsType().getValue().ifPresent(credentialsType -> {
        switch (credentialsType) {
          case EC2_IAM: {
            config.setUseEc2IamCredentials(true);
            config.setAccessKey(null);
            config.setEncryptedSecretKey(null);
            input.getEc2IamCredentials()
                .getValue()
                .flatMap(credentials -> credentials.getDelegateSelector().getValue())
                .ifPresent(config::setTag);
          } break;
          case MANUAL: {
            config.setUseEc2IamCredentials(false);
            config.setTag(null);
            input.getManualCredentials().getValue().ifPresent(credentials -> {
              credentials.getAccessKey().getValue().ifPresent(config::setAccessKey);
              credentials.getSecretKeySecretId().getValue().ifPresent(config::setEncryptedSecretKey);
            });
          } break;
          default:
            throw new InvalidRequestException("Invalid credentials type");
        }
      });
    }

    if (input.getCrossAccountAttributes().isPresent()) {
      input.getCrossAccountAttributes().getValue().ifPresent(crossAccountAttributes -> {
        crossAccountAttributes.getAssumeCrossAccountRole().getValue().ifPresent(config::setAssumeCrossAccountRole);

        AwsCrossAccountAttributes awsCrossAccountAttributes =
            ObjectUtils.defaultIfNull(config.getCrossAccountAttributes(), AwsCrossAccountAttributes.builder().build());

        crossAccountAttributes.getCrossAccountRoleArn().getValue().ifPresent(
            awsCrossAccountAttributes::setCrossAccountRoleArn);
        crossAccountAttributes.getExternalId().getValue().ifPresent(awsCrossAccountAttributes::setExternalId);

        config.setCrossAccountAttributes(awsCrossAccountAttributes);
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
