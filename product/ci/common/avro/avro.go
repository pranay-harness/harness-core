package avro

import (
	"github.com/linkedin/goavro/v2"
	"github.com/pkg/errors"

	"github.com/wings-software/portal/product/ci/common/avro/schema"
)

//Serialzer is the interface for encoding and decoding structs
type Serialzer interface {
	// Serialize given struct and return the binary value
	Serialize(datum interface{}) ([]byte, error)
	// Deserialize given struct and return the decoded interface{}
	Deserialize(buf []byte) (interface{}, error)
}

// CgphSerialzer struct implementing NewCgphSer interface
type CgphSerialzer struct {
	codec *goavro.Codec
}

// NewCgphSerialzer returns new CgphSerialzer object with the codec
// based on the schema received in the input
func NewCgphSerialzer(file string) (*CgphSerialzer, error) {
	schema, err := schema.Asset(file)
	if err != nil {
		return nil, errors.Wrap(err, "failed to read schema file")
	}

	codec, err := goavro.NewCodec(string(schema))
	if err != nil {
		panic(err)
	}

	return &CgphSerialzer{
		codec: codec,
	}, nil
}

//Serialize a given struct interface and return byte array and error
func (c *CgphSerialzer) Serialize(datum interface{}) ([]byte, error) {
	bin, err := c.codec.BinaryFromNative(nil, datum)
	if err != nil {
		return nil, errors.Wrap(err, "failed to encode the data")
	}
	return bin, nil
}

//Deserialize a interface and return a Byte array which can be converted into corresponding struct
func (c *CgphSerialzer) Deserialize(buf []byte) (interface{}, error) {
	native, _, err := c.codec.NativeFromBinary(buf)
	if err != nil {
		return nil, err
	}
	return native, nil
}
