// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package handler

import (
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"
	"time"

	"github.com/dchest/authcookie"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/product/log-service/config"
)

type MockHandler struct{}

func (*MockHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {}

func TestTokenGenerationMiddleware_Success(t *testing.T) {
	var config config.Config
	globalToken := "token"
	config.Secrets.GlobalToken = globalToken
	v := url.Values{}
	v.Add("accountID", "account")
	header := http.Header{}
	header.Add(authHeader, globalToken)
	httpReq := &http.Request{Form: v, Header: header}
	fn := TokenGenerationMiddleware(config, true)
	mockHandler := &MockHandler{}
	handlerFunc := fn(mockHandler)
	writer := httptest.NewRecorder()
	handlerFunc.ServeHTTP(writer, httpReq)
	assert.Equal(t, writer.Code, 200)
}

func TestTokenGenerationMiddleware_TokenInURL_Success(t *testing.T) {
	var config config.Config
	globalToken := "token"
	config.Secrets.GlobalToken = globalToken
	v := url.Values{}
	v.Add("accountID", "account")
	v.Add(authHeader, globalToken)
	header := http.Header{}
	httpReq := &http.Request{Form: v, Header: header}
	fn := TokenGenerationMiddleware(config, true)
	mockHandler := &MockHandler{}
	handlerFunc := fn(mockHandler)
	writer := httptest.NewRecorder()
	handlerFunc.ServeHTTP(writer, httpReq)
	assert.Equal(t, writer.Code, 200)
}

func TestTokenGenerationMiddleware_IncorrectToken(t *testing.T) {
	var config config.Config
	globalToken := "token"
	config.Secrets.GlobalToken = globalToken
	v := url.Values{}
	v.Add("accountID", "account")
	header := http.Header{}
	header.Add(authHeader, "incorrect_token")
	httpReq := &http.Request{Form: v, Header: header}
	fn := TokenGenerationMiddleware(config, true)
	mockHandler := &MockHandler{}
	handlerFunc := fn(mockHandler)
	writer := httptest.NewRecorder()
	handlerFunc.ServeHTTP(writer, httpReq)
	assert.Equal(t, writer.Code, 400)
}

func TestTokenGenerationMiddleware_AccountIDAbsent(t *testing.T) {
	var config config.Config
	globalToken := "token"
	config.Secrets.GlobalToken = globalToken
	header := http.Header{}
	header.Add(authHeader, "token")
	httpReq := &http.Request{Header: header}
	fn := TokenGenerationMiddleware(config, true)
	mockHandler := &MockHandler{}
	handlerFunc := fn(mockHandler)
	writer := httptest.NewRecorder()
	handlerFunc.ServeHTTP(writer, httpReq)
	assert.Equal(t, writer.Code, 400)
}

func TestTokenGenerationMiddleware_SkipAccountIDCheck(t *testing.T) {
	var config config.Config
	globalToken := "token"
	config.Secrets.GlobalToken = globalToken
	header := http.Header{}
	header.Add(authHeader, "token")
	httpReq := &http.Request{Header: header}
	fn := TokenGenerationMiddleware(config, false)
	mockHandler := &MockHandler{}
	handlerFunc := fn(mockHandler)
	writer := httptest.NewRecorder()
	handlerFunc.ServeHTTP(writer, httpReq)
	assert.Equal(t, writer.Code, 200)
}

func TestAuthMiddleware_Success(t *testing.T) {
	var config config.Config
	logSecret := "secret"
	accountID := "account"
	config.Secrets.LogSecret = logSecret
	cookie := authcookie.NewSinceNow(accountID, 1*time.Hour, []byte(logSecret))
	header := http.Header{}
	v := url.Values{}
	v.Add("accountID", accountID)
	v.Add("key", "key")
	header.Add(authHeader, cookie)
	httpReq := &http.Request{Form: v, Header: header}
	fn := AuthMiddleware(config)
	mockHandler := &MockHandler{}
	handlerFunc := fn(mockHandler)
	writer := httptest.NewRecorder()
	handlerFunc.ServeHTTP(writer, httpReq)
	assert.Equal(t, writer.Code, 200)
}

func TestAuthMiddleware_TokenInURL_Success(t *testing.T) {
	var config config.Config
	logSecret := "secret"
	accountID := "account"
	config.Secrets.LogSecret = logSecret
	cookie := authcookie.NewSinceNow(accountID, 1*time.Hour, []byte(logSecret))
	header := http.Header{}
	v := url.Values{}
	v.Add("accountID", accountID)
	v.Add("key", "key")
	v.Add(authHeader, cookie)
	httpReq := &http.Request{Form: v, Header: header}
	fn := AuthMiddleware(config)
	mockHandler := &MockHandler{}
	handlerFunc := fn(mockHandler)
	writer := httptest.NewRecorder()
	handlerFunc.ServeHTTP(writer, httpReq)
	assert.Equal(t, writer.Code, 200)
}

func TestAuthMiddleware_IncorrectSecret(t *testing.T) {
	var config config.Config
	logSecret := "secret"
	incorrectLogSecret := "notsecret"
	accountID := "account"
	config.Secrets.LogSecret = logSecret
	// Generate cookie with a different secret
	cookie := authcookie.NewSinceNow(accountID, 1*time.Hour, []byte(incorrectLogSecret))
	header := http.Header{}
	v := url.Values{}
	v.Add("accountID", accountID)
	v.Add("key", "key")
	header.Add(authHeader, cookie)
	httpReq := &http.Request{Form: v, Header: header}
	fn := AuthMiddleware(config)
	mockHandler := &MockHandler{}
	handlerFunc := fn(mockHandler)
	writer := httptest.NewRecorder()
	handlerFunc.ServeHTTP(writer, httpReq)
	assert.Equal(t, writer.Code, 400)
}

func TestAuthMiddleware_IncorrectAccount(t *testing.T) {
	var config config.Config
	logSecret := "secret"
	incorrectaccountID := "notaccount"
	accountID := "account"
	config.Secrets.LogSecret = logSecret
	// Generate cookie with a different account
	cookie := authcookie.NewSinceNow(incorrectaccountID, 1*time.Hour, []byte(logSecret))
	header := http.Header{}
	v := url.Values{}
	v.Add("accountID", accountID)
	v.Add("key", "key")
	header.Add(authHeader, cookie)
	httpReq := &http.Request{Form: v, Header: header}
	fn := AuthMiddleware(config)
	mockHandler := &MockHandler{}
	handlerFunc := fn(mockHandler)
	writer := httptest.NewRecorder()
	handlerFunc.ServeHTTP(writer, httpReq)
	assert.Equal(t, writer.Code, 400)
}

func TestAuthMiddleware_NoKeyPresent(t *testing.T) {
	var config config.Config
	logSecret := "secret"
	incorrectaccountID := "notaccount"
	accountID := "account"
	config.Secrets.LogSecret = logSecret
	// Generate cookie with a different account
	cookie := authcookie.NewSinceNow(incorrectaccountID, 1*time.Hour, []byte(logSecret))
	header := http.Header{}
	v := url.Values{}
	v.Add("accountID", accountID)
	header.Add(authHeader, cookie)
	httpReq := &http.Request{Form: v, Header: header}
	fn := AuthMiddleware(config)
	mockHandler := &MockHandler{}
	handlerFunc := fn(mockHandler)
	writer := httptest.NewRecorder()
	handlerFunc.ServeHTTP(writer, httpReq)
	assert.Equal(t, writer.Code, 400)
}
