// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package mock

//go:generate mockgen -source=../store/store.go -package=mock -destination=mock_store.go Store
//go:generate mockgen -source=../stream/stream.go -package=mock -destination=mock_stream.go Stream
//go:generate mockgen -source=../client/client.go -package=mock -destination=mock_client.go Client
