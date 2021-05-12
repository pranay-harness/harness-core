package handler

import (
	"io"
	"net/http"

	"go.uber.org/zap"

	"github.com/wings-software/portal/product/ci/ti-service/config"
	"github.com/wings-software/portal/product/ci/ti-service/db"
	"github.com/wings-software/portal/product/ci/ti-service/tidb"

	"github.com/go-chi/chi"
)

// Handler returns an http.Handler that exposes the
// service resources.
func Handler(db db.Db, tidb tidb.TiDB, config config.Config, log *zap.SugaredLogger) http.Handler {
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

		sr.Post("/write", HandleWrite(db, log))
		sr.Get("/summary", HandleSummary(db, log))
		sr.Get("/test_cases", HandleTestCases(db, log))
		sr.Get("/test_suites", HandleTestSuites(db, log))
		return sr
	}())

	r.Mount("/tests", func() http.Handler {
		sr := chi.NewRouter()
		// Validate the accountId in URL with the token generated above and authorize the request
		if !config.Secrets.DisableAuth {
			sr.Use(AuthMiddleware(config))
		}

		sr.Post("/select", HandleSelect(tidb, db, log))
		sr.Get("/overview", HandleOverview(db, log))
		sr.Post("/uploadcg", HandleUploadCg(tidb, db, log))
		return sr
	}())

	// Health check
	r.Get("/healthz", func(w http.ResponseWriter, r *http.Request) {
		io.WriteString(w, "OK")
	})

	return r
}
