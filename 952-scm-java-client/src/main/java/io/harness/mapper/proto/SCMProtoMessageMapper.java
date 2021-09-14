/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mapper.proto;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CommitDetails;
import io.harness.beans.WebhookGitUser;
import io.harness.product.ci.scm.proto.Commit;
import io.harness.product.ci.scm.proto.Repository;
import io.harness.product.ci.scm.proto.Signature;
import io.harness.product.ci.scm.proto.User;

import com.google.protobuf.Timestamp;
import java.util.Optional;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DX)
public class SCMProtoMessageMapper {
  public User convertWebhookGitUser(WebhookGitUser webhookGitUser) {
    return User.newBuilder()
        .setAvatar(Optional.ofNullable(webhookGitUser.getAvatar()).orElse(""))
        .setEmail(Optional.ofNullable(webhookGitUser.getEmail()).orElse(""))
        .setLogin(Optional.ofNullable(webhookGitUser.getGitId()).orElse(""))
        .setName(Optional.ofNullable(webhookGitUser.getName()).orElse(""))
        .build();
  }

  public Repository convertRepository(io.harness.beans.Repository repo) {
    return Repository.newBuilder()
        .setId(Optional.ofNullable(repo.getId()).orElse(""))
        .setName(Optional.ofNullable(repo.getName()).orElse(""))
        .setNamespace(Optional.ofNullable(repo.getNamespace()).orElse(""))
        .setLink(Optional.ofNullable(repo.getLink()).orElse(""))
        .setBranch(Optional.ofNullable(repo.getBranch()).orElse(""))
        .setPrivate(repo.isPrivate())
        .setClone(Optional.ofNullable(repo.getHttpURL()).orElse(""))
        .setCloneSsh(Optional.ofNullable(repo.getSshURL()).orElse(""))
        .build();
  }

  public Commit convertCommitDetails(CommitDetails commit) {
    return Commit.newBuilder()
        .setSha(Optional.ofNullable(commit.getCommitId()).orElse(""))
        .setMessage(Optional.ofNullable(commit.getMessage()).orElse(""))
        .setLink(Optional.ofNullable(commit.getLink()).orElse(""))
        .setCommitter(Signature.newBuilder()
                          .setDate(Timestamp.newBuilder().setSeconds(commit.getTimeStamp() / 1000).build())
                          .build())
        .setAuthor(Signature.newBuilder()
                       .setEmail(Optional.ofNullable(commit.getOwnerEmail()).orElse(""))
                       .setLogin(Optional.ofNullable(commit.getOwnerId()).orElse(""))
                       .setName(Optional.ofNullable(commit.getOwnerName()).orElse(""))
                       .build())
        .build();
  }
}
