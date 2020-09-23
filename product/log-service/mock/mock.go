package mock

//go:generate mockgen -package=mock -destination=mock_store.go github.com/wings-software/log-service/store Store
//go:generate mockgen -package=mock -destination=mock_stream.go github.com/wings-software/log-service/stream Stream
//go:generate mockgen -package=mock -destination=mock_client.go github.com/wings-software/log-service/client Client
