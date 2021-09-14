/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.ci;

import static io.harness.rule.OwnerRule.HARSH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cistatus.GithubAppTokenCreationResponse;
import io.harness.cistatus.StatusCreationResponse;
import io.harness.cistatus.service.GithubRestClient;
import io.harness.cistatus.service.GithubServiceImpl;
import io.harness.cistatus.service.bitbucket.BitbucketRestClient;
import io.harness.cistatus.service.bitbucket.BitbucketServiceImpl;
import io.harness.cistatus.service.gitlab.GitlabRestClient;
import io.harness.cistatus.service.gitlab.GitlabServiceImpl;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.ci.status.BuildStatusPushResponse;
import io.harness.delegate.beans.ci.status.BuildStatusPushResponse.Status;
import io.harness.delegate.task.ci.CIBuildStatusPushParameters;
import io.harness.delegate.task.ci.CIBuildStatusPushTask;
import io.harness.delegate.task.ci.GitSCMType;
import io.harness.rule.Owner;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import retrofit2.Call;
import retrofit2.Response;

public class CIBuildStatusPushTaskTest extends CategoryTest {
  private GithubRestClient githubRestClient;
  private GitlabRestClient gitlabRestClient;
  private BitbucketRestClient bitbucketRestClient;

  private GithubServiceImpl githubServiceImpl;
  private GitlabServiceImpl gitlabServiceImpl;
  private BitbucketServiceImpl bitbucketServiceImpl;

  private final String APP_ID = "APP_ID";
  private final String DESC = "desc";
  private final String STATE = "success";
  private final String BUILD_NUMBER = "buildNumber";
  private final String TITLE = "title";
  private final String REPO = "repo";
  private final String OWNER = "owner";
  private final String USERNAME = "user";
  private final String TOKEN = "token";
  private final String SHA = "e9a0d31c5ac677ec1e06fb3ab69cd1d2cc62a74a";
  private final String IDENTIFIER = "stageIdentifier";
  private final String INSTALL_ID = "123";
  private final String TARGET_URL = "https://app.harness.io";
  private final String KEY = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDM5xLSJEkinAIL\n"
      + "qxjE965bAaZLoBuLJiKdkBnrckdSfGgqdilHSkaVEgRSnG7JQcumahbt/7nOV7Yk\n"
      + "BPFPOtfphikGZVgM9yNPn+PH7jEtXTnnkSClwq+onWcOFSlc5/17EWDTk5xVMdqx\n"
      + "m6WfgF+JdCah1+GoVAmmm4G0yub/00ZCKooItQzz0BQm9qEHpHSre5sCt7wNgtH3\n"
      + "RaD+dQKRJMAfcpWdAJcjIKPX6HlhfOMTb49vDLDTMgScZT17eOgr8QvedGgye5bz\n"
      + "MOWhsRnjS4cgGbcTq1ftGYE4neIX7TbyX0SEt8gg7MXe+tJjMh/+7+bTRUKQYJpC\n"
      + "sPQFTrQBAgMBAAECggEAYU9MLOh2oy0b+5aiCMjn0OiTpU7ARfEyd0m8RYjcPlw+\n"
      + "zAuZxvWLV7havTD1nDbXFI1FnnnYMBqPscN3Jn13lLvWN+dhTacA6guxDX4ddMHV\n"
      + "ghf2PUKcUaOPEa0TG8BBLXUvWsu7bupiRf75RSqeNJUo06vGyz495xXrH4VM9yjx\n"
      + "nTOkddwl1LK07KJ95zeMUn4o+NkaWMUboEUGlC2hPdFAk/EWatgzXOVNvcWHeceK\n"
      + "tWBAPyIy4Q97sNeFiuvcs5LL7dZ19ITUMcVbAS9CxrxhjVmqpFJo5AEY+SZe+WbN\n"
      + "36Dm/WSHV1LXZVzJkEkRiZBrDtp+hMp57CFBg+z5wQKBgQDpTIvIFm1LL6Bk7/+T\n"
      + "Uwr91Lpcv861w65KoX2ekrXla27mSfTK4AOph0E5DTNC6UdGfeGDhmuJ0d/+Bb/z\n"
      + "6Yz+dnVcQXGsYBBCjhbZt6dLYDpSSffFU4hp707IkfneM3a4uCTH2kKyUpLKi4Q2\n"
      + "Vv6ELGV//u0HMRQ3EtIREuKRHQKBgQDg1y08lnMW8G7n+Uf34NbDDYwDrGfUj2L8\n"
      + "OEOYAqCSA9XHGolBPzhcinZ5q6fYR9qBd20qWMz1oJsf88LQA8/iebTv7cuHMHWW\n"
      + "u/Jcqhf8uJITbZSrQs8nlKACGYCUhoy9aNvX2PucJAHAsgSu9OtPmlSJxqUs3nOE\n"
      + "VFTSY2H9NQKBgQCOjWw4BaQgtehO5OsIrUxhD1QUiksXe4sLJSQp+cFVftDTvErs\n"
      + "j/cM5o1u++bfssUPiKl8gW1CWFCC2iaRNpslfWJ2zbJUvpoQ4NuLixGZGCJq17Gj\n"
      + "DEilWkmMes3v/QhFFJe82lu4tIXnZ1qRDZUVVD9s92sD4vRUNpbPQffY7QKBgQDR\n"
      + "4sxNtLw2+7bsQV4XXQHeDzVW8If0eu2SOQuQSVOPOplDRdg+2j9I09CI/96tHVYy\n"
      + "aUO0tjSOTqDAkRKYkBZteeOX3cmSp3/9d/Fk4zuFJN7n1/FidflfH3TGwPuwqnGT\n"
      + "FuGyetFWDp68PPH2SJepNY4ZFyB15Cq9quOLik6cyQKBgChmTS4xeMeQsCyLdU0e\n"
      + "BnXIseynAoLTUwnrAPs53NXSLlbS9zCLJvDCDegHpLz5g4fD4pyB+/i5h3Pbm7Q5\n"
      + "pkfEfyhF0NRY9eOy059rRQplTvpK+u+vnA9wLL6iIMgfQTcVWkaz8GJH9H1Y5eQq\n"
      + "BBqMchCoiaQTrwy010MSgqyV";

  @InjectMocks
  private CIBuildStatusPushTask ciBuildStatusPushTask = new CIBuildStatusPushTask(
      DelegateTaskPackage.builder().data(TaskData.builder().build()).build(), null, null, null);

  @Before
  public void setUp() throws IllegalAccessException {
    githubRestClient = Mockito.mock(GithubRestClient.class);
    gitlabRestClient = Mockito.mock(GitlabRestClient.class);
    bitbucketRestClient = Mockito.mock(BitbucketRestClient.class);
    githubServiceImpl = spy(new GithubServiceImpl());
    gitlabServiceImpl = spy(new GitlabServiceImpl());
    bitbucketServiceImpl = spy(new BitbucketServiceImpl());
    doReturn(githubRestClient).when(githubServiceImpl).getGithubClient(any());
    doReturn(gitlabRestClient).when(gitlabServiceImpl).getGitlabRestClient(any(), any());
    doReturn(bitbucketRestClient).when(bitbucketServiceImpl).getBitbucketClient(any(), any());
    on(ciBuildStatusPushTask).set("githubService", githubServiceImpl);
    on(ciBuildStatusPushTask).set("gitlabService", gitlabServiceImpl);
    on(ciBuildStatusPushTask).set("bitbucketService", bitbucketServiceImpl);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  @Ignore("Recreate test object after pms integration")
  public void testDelegatePushStatusForGithub() throws IOException {
    GithubAppTokenCreationResponse githubAppTokenCreationResponse =
        GithubAppTokenCreationResponse.builder().token("token").build();
    Call<GithubAppTokenCreationResponse> githubAppTokenCreationResponseCall = mock(Call.class);
    when(githubAppTokenCreationResponseCall.execute()).thenReturn(Response.success(githubAppTokenCreationResponse));

    when(githubRestClient.createAccessToken(anyString(), anyString())).thenReturn(githubAppTokenCreationResponseCall);

    StatusCreationResponse statusCreationResponse = StatusCreationResponse.builder().build();
    Call<StatusCreationResponse> githubStatusCreationResponseCall = mock(Call.class);
    when(githubStatusCreationResponseCall.execute()).thenReturn(Response.success(statusCreationResponse));

    when(githubRestClient.createStatus(anyString(), anyString(), anyString(), anyString(), anyMap()))
        .thenReturn(githubStatusCreationResponseCall);

    BuildStatusPushResponse buildStatusPushResponse =
        (BuildStatusPushResponse) ciBuildStatusPushTask.run(CIBuildStatusPushParameters.builder()
                                                                .appId(APP_ID)
                                                                .sha(SHA)
                                                                .key(KEY)
                                                                .identifier(IDENTIFIER)
                                                                .buildNumber(BUILD_NUMBER)
                                                                .installId(INSTALL_ID)
                                                                .gitSCMType(GitSCMType.GITHUB)
                                                                .owner(OWNER)
                                                                .repo(REPO)
                                                                .state(STATE)
                                                                .title(TITLE)
                                                                .target_url(TARGET_URL)
                                                                .desc(DESC)
                                                                .build());

    assertThat(buildStatusPushResponse.getStatus()).isEqualTo(Status.SUCCESS);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  @Ignore("Recreate test object after pms integration")
  public void testDelegatePushStatusForBitbucket() throws IOException {
    StatusCreationResponse statusCreationResponse = StatusCreationResponse.builder().build();
    Call<StatusCreationResponse> statusCreationResponseCall = mock(Call.class);
    when(statusCreationResponseCall.execute()).thenReturn(Response.success(statusCreationResponse));

    when(bitbucketRestClient.createStatus(anyString(), anyString(), anyString(), anyString(), anyMap()))
        .thenReturn(statusCreationResponseCall);

    BuildStatusPushResponse buildStatusPushResponse =
        (BuildStatusPushResponse) ciBuildStatusPushTask.run(CIBuildStatusPushParameters.builder()
                                                                .appId(APP_ID)
                                                                .sha(SHA)
                                                                .key(KEY)
                                                                .identifier(IDENTIFIER)
                                                                .buildNumber(BUILD_NUMBER)
                                                                .installId(INSTALL_ID)
                                                                .owner(OWNER)
                                                                .userName(USERNAME)
                                                                .gitSCMType(GitSCMType.BITBUCKET)
                                                                .token(TOKEN)
                                                                .repo(REPO)
                                                                .state(STATE)
                                                                .title(TITLE)
                                                                .target_url(TARGET_URL)
                                                                .desc(DESC)
                                                                .build());

    assertThat(buildStatusPushResponse.getStatus()).isEqualTo(Status.SUCCESS);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testErrorStatePushStatusForBitbucket() throws IOException {
    StatusCreationResponse statusCreationResponse = StatusCreationResponse.builder().build();
    Call<StatusCreationResponse> statusCreationResponseCall = mock(Call.class);
    when(statusCreationResponseCall.execute())
        .thenReturn(Response.error(ResponseBody.create(MediaType.parse("text/plain"), "MSG"),
            new okhttp3.Response
                .Builder() //
                .code(401)
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://localhost/").build())
                .message("err")
                .build()));

    when(bitbucketRestClient.createStatus(anyString(), anyString(), anyString(), anyString(), anyMap()))
        .thenReturn(statusCreationResponseCall);

    BuildStatusPushResponse buildStatusPushResponse =
        (BuildStatusPushResponse) ciBuildStatusPushTask.run(CIBuildStatusPushParameters.builder()
                                                                .appId(APP_ID)
                                                                .sha(SHA)
                                                                .key(KEY)
                                                                .identifier(IDENTIFIER)
                                                                .buildNumber(BUILD_NUMBER)
                                                                .installId(INSTALL_ID)
                                                                .owner(OWNER)
                                                                .userName(USERNAME)
                                                                .gitSCMType(GitSCMType.BITBUCKET)
                                                                .token(TOKEN)
                                                                .repo(REPO)
                                                                .state(STATE)
                                                                .title(TITLE)
                                                                .target_url(TARGET_URL)
                                                                .desc(DESC)
                                                                .build());

    assertThat(buildStatusPushResponse.getStatus()).isEqualTo(Status.ERROR);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  @Ignore("Recreate test object after pms integration")
  public void testDelegatePushStatusForGitlab() throws IOException {
    StatusCreationResponse statusCreationResponse = StatusCreationResponse.builder().build();
    Call<StatusCreationResponse> statusCreationResponseCall = mock(Call.class);
    when(statusCreationResponseCall.execute()).thenReturn(Response.success(statusCreationResponse));

    when(gitlabRestClient.createStatus(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(statusCreationResponseCall);

    BuildStatusPushResponse buildStatusPushResponse =
        (BuildStatusPushResponse) ciBuildStatusPushTask.run(CIBuildStatusPushParameters.builder()
                                                                .appId(APP_ID)
                                                                .sha(SHA)
                                                                .key(KEY)
                                                                .identifier(IDENTIFIER)
                                                                .buildNumber(BUILD_NUMBER)
                                                                .installId(INSTALL_ID)
                                                                .owner(OWNER)
                                                                .userName(USERNAME)
                                                                .gitSCMType(GitSCMType.GITLAB)
                                                                .token(TOKEN)
                                                                .repo(REPO)
                                                                .state(STATE)
                                                                .title(TITLE)
                                                                .target_url(TARGET_URL)
                                                                .desc(DESC)
                                                                .build());

    assertThat(buildStatusPushResponse.getStatus()).isEqualTo(Status.SUCCESS);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testErrorStatePushStatusForGitlab() throws IOException {
    StatusCreationResponse statusCreationResponse = StatusCreationResponse.builder().build();
    Call<StatusCreationResponse> statusCreationResponseCall = mock(Call.class);
    when(statusCreationResponseCall.execute())
        .thenReturn(Response.error(ResponseBody.create(MediaType.parse("text/plain"), "MSG"),
            new okhttp3.Response
                .Builder() //
                .code(401)
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://localhost/").build())
                .message("err")
                .build()));

    when(gitlabRestClient.createStatus(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(statusCreationResponseCall);

    BuildStatusPushResponse buildStatusPushResponse =
        (BuildStatusPushResponse) ciBuildStatusPushTask.run(CIBuildStatusPushParameters.builder()
                                                                .appId(APP_ID)
                                                                .sha(SHA)
                                                                .key(KEY)
                                                                .identifier(IDENTIFIER)
                                                                .buildNumber(BUILD_NUMBER)
                                                                .installId(INSTALL_ID)
                                                                .owner(OWNER)
                                                                .userName(USERNAME)
                                                                .gitSCMType(GitSCMType.GITLAB)
                                                                .token(TOKEN)
                                                                .repo(REPO)
                                                                .state(STATE)
                                                                .title(TITLE)
                                                                .target_url(TARGET_URL)
                                                                .desc(DESC)
                                                                .build());

    assertThat(buildStatusPushResponse.getStatus()).isEqualTo(Status.ERROR);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testDelegatePushStatusERRORForGithub() throws IOException {
    Call<GithubAppTokenCreationResponse> githubAppTokenCreationResponseCall = mock(Call.class);

    when(githubAppTokenCreationResponseCall.execute())
        .thenReturn(Response.error(ResponseBody.create(MediaType.parse("text/plain"), "MSG"),
            new okhttp3.Response
                .Builder() //
                .code(401)
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://localhost/").build())
                .message("err")
                .build()));

    when(githubRestClient.createAccessToken(anyString(), anyString())).thenReturn(githubAppTokenCreationResponseCall);

    BuildStatusPushResponse buildStatusPushResponse =
        (BuildStatusPushResponse) ciBuildStatusPushTask.run(CIBuildStatusPushParameters.builder()
                                                                .appId(APP_ID)
                                                                .sha(SHA)
                                                                .key(KEY)
                                                                .identifier(IDENTIFIER)
                                                                .buildNumber(BUILD_NUMBER)
                                                                .installId(INSTALL_ID)
                                                                .owner(OWNER)
                                                                .repo(REPO)
                                                                .gitSCMType(GitSCMType.GITHUB)
                                                                .state(STATE)
                                                                .title(TITLE)
                                                                .target_url(TARGET_URL)
                                                                .desc(DESC)
                                                                .build());

    assertThat(buildStatusPushResponse.getStatus()).isEqualTo(Status.ERROR);
  }
}
