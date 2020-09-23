package handler

import (
	"net/http"

	cihandler "github.com/wings-software/portal/product/log-service/handler/ci"
	"github.com/wings-software/portal/product/log-service/logger"
	"github.com/wings-software/portal/product/log-service/store"
	"github.com/wings-software/portal/product/log-service/stream"

	"github.com/go-chi/chi"
)

// Handler returns an http.Handler that exposes the
// service resources.
func Handler(stream stream.Stream, store store.Store) http.Handler {
	r := chi.NewRouter()
	r.Use(logger.Middleware)

	// Add routes for all endpoints
	cihandler.AddRoutes(stream, store, r)

	return r
}
