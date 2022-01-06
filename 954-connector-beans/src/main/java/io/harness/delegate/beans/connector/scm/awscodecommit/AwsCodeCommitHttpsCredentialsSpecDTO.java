/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.scm.awscodecommit;

import static io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnectorConstants.ACCESS_KEY_AND_SECRET_KEY;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes(
    { @JsonSubTypes.Type(value = AwsCodeCommitSecretKeyAccessKeyDTO.class, name = ACCESS_KEY_AND_SECRET_KEY) })
@Schema(name = "AwsCodeCommitHttpsCredentialsSpec",
    description =
        "This contains details of the AWS Code Commit credentials specs such as references of username and password used via HTTPS connections")
public interface AwsCodeCommitHttpsCredentialsSpecDTO extends DecryptableEntity {}
