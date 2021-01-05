package handler

import (
	"go.uber.org/zap"
	"io"
	"net/http"

	"github.com/wings-software/portal/product/ci/ti-service/config"
	"github.com/wings-software/portal/product/ci/ti-service/db"

	"github.com/go-chi/chi"
)

// Handler returns an http.Handler that exposes the
// service resources.
func Handler(db db.Db, config config.Config, log *zap.SugaredLogger) http.Handler {
	r := chi.NewRouter()

	// Token generation endpoints
	// Format: /token?accountId=
	r.Mount("/token", func() http.Handler {
		sr := chi.NewRouter()
		// Validate the incoming request with a global secret and return back a token
		// for the given account ID if the match is successful.
		sr.Use(TokenGenerationMiddleware(config, true))

		sr.Get("/", HandleToken(config))
		return sr
	}()) // Validates against global token

	r.Mount("/reports", func() http.Handler {
		sr := chi.NewRouter()
		// Validate the accountId in URL with the token generated above and authorize the request
		sr.Use(AuthMiddleware(config))

		sr.Post("/write", HandleWrite(db, config, log))
		sr.Get("/summary", HandleSummary(db, config, log))
		sr.Get("/test_cases", HandleTestCases(db, config, log))
		sr.Get("/test_suites", HandleTestSuites(db, config, log))
		return sr
	}())

	// Health check
	r.Get("/healthz", func(w http.ResponseWriter, r *http.Request) {
		io.WriteString(w, "OK")
	})

	return r
}
