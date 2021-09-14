// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package handler

import (
	"io"
	"net/http"

	"github.com/wings-software/portal/product/ci/ti-service/config"
	"github.com/wings-software/portal/product/ci/ti-service/db"
	"github.com/wings-software/portal/product/ci/ti-service/logger"
	"github.com/wings-software/portal/product/ci/ti-service/tidb"

	"github.com/go-chi/chi"
)

// Handler returns an http.Handler that exposes the
// service resources.
func Handler(db db.Db, tidb tidb.TiDB, config config.Config) http.Handler {
	r := chi.NewRouter()

	// Token generation endpoints
	// Format: /token?accountId=
	r.Mount("/token", func() http.Handler {
		sr := chi.NewRouter()
		// Validate the incoming request with a global secret and return back a token
		// for the given account ID if the match is successful.
		if !config.Secrets.DisableAuth {
			sr.Use(TokenGenerationMiddleware(config, true))
		}

		sr.Get("/", HandleToken(config))
		return sr
	}()) // Validates against global token

	r.Mount("/reports", func() http.Handler {
		sr := chi.NewRouter()
		// Validate the accountId in URL with the token generated above and authorize the request
		if !config.Secrets.DisableAuth {
			sr.Use(AuthMiddleware(config))
		}
		// use the logging middleware
		sr.Use(logger.Middleware)

		sr.Get("/info", HandleReportsInfo(db))
		sr.Post("/write", HandleWrite(db))
		sr.Get("/summary", HandleSummary(db))
		sr.Get("/test_cases", HandleTestCases(db))
		sr.Get("/test_suites", HandleTestSuites(db))
		return sr
	}())

	r.Mount("/tests", func() http.Handler {
		sr := chi.NewRouter()
		// use the logging middleware
		sr.Use(logger.Middleware)
		// Validate the accountId in URL with the token generated above and authorize the request
		if !config.Secrets.DisableAuth {
			sr.Use(AuthMiddleware(config))
		}

		sr.Get("/info", HandleIntelligenceInfo(db))
		sr.Post("/select", HandleSelect(tidb, db, config))
		sr.Get("/overview", HandleOverview(db))
		sr.Post("/uploadcg", HandleUploadCg(tidb, db))
		return sr
	}())

	r.Mount("/vg", func() http.Handler {
		sr := chi.NewRouter()
		// Validate the accountId in URL with the token generated above and authorize the request
		if !config.Secrets.DisableAuth {
			sr.Use(AuthMiddleware(config))
		}

		sr.Get("/", HandleVgSearch(tidb, db))

		return sr
	}())

	// Health check
	r.Get("/healthz", func(w http.ResponseWriter, r *http.Request) {
		io.WriteString(w, "OK")
	})

	// Readiness check
	r.Mount("/ready/healthz", func() http.Handler {
		sr := chi.NewRouter()
		sr.Get("/", HandlePing(db))

		return sr
	}())

	return r
}
