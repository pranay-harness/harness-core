// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package logger

import (
	"net/http"

	"github.com/gofrs/uuid"
	"github.com/sirupsen/logrus"
)

// Middleware provides logging middleware.
func Middleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		id := r.Header.Get("X-Request-ID")
		if id == "" {
			uuid, _ := uuid.NewV4()
			id = uuid.String()
		}
		ctx := r.Context()
		log := FromContext(ctx).WithField("request-id", id)
		accountID := r.FormValue("accountID")
		log = log.WithFields(logrus.Fields{
			"accountID": accountID,
			"method":    r.Method,
			"request":   r.RequestURI,
			"remote":    r.RemoteAddr,
		})
		ctx = WithContext(ctx, log)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}
