// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package handler

import (
	"errors"
	"fmt"
	"net/http"

	"github.com/dchest/authcookie"
	"github.com/wings-software/portal/product/ci/ti-service/config"
)

const authHeader = "X-Harness-Token"

// TokenGenerationMiddleware is middleware to ensure that the incoming request is allowed to
// invoke token-generation endpoints.
func TokenGenerationMiddleware(config config.Config, validateAccount bool) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			if validateAccount {
				accountID := r.FormValue("accountId")
				if accountID == "" {
					WriteBadRequest(w, errors.New("no account ID in query params"))
					return
				}
			}

			// Get token from header
			inputToken := r.Header.Get(authHeader)
			if inputToken == "" {
				WriteBadRequest(w, errors.New("no token in header"))
				return
			}

			if inputToken != config.Secrets.GlobalToken {
				// Error: invalid token
				WriteBadRequest(w, errors.New("token in request not authorized for receiving tokens"))
				return
			}

			next.ServeHTTP(w, r)
		})
	}
}

// GetAuthFunc is middleware to ensure that the incoming request is allowed to access resources
// at the specific accountID
func AuthMiddleware(config config.Config) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {

			accountID := r.FormValue("accountId")
			if accountID == "" {
				WriteBadRequest(w, errors.New("no account ID in query params"))
				return
			}

			// Get token from header
			inputToken := r.Header.Get(authHeader)
			if inputToken == "" {
				WriteBadRequest(w, errors.New("no token in header"))
				return
			}
			// accountID in token should be same as accountID in URL
			secret := []byte(config.Secrets.LogSecret)
			login := authcookie.Login(inputToken, secret)
			if login == "" || login != accountID {
				WriteBadRequest(w, errors.New(fmt.Sprintf("operation not permitted for accountId: %s", accountID)))
				return
			}

			next.ServeHTTP(w, r)
		})
	}
}
