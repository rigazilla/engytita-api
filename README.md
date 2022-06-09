# ENGYTITA API
This repository contains the API definition for the Engytita project.

Design guidelines from Envoy project are generally used as reference otherwise specified. See [this doc set](https://github.com/envoyproxy/envoy/tree/main/api#further-api-reading)

Some core ideas:
- API verbs and configuration are described independently. This to allow design where an actor can self discover its own config somewhere around.
- canonical language for API spec is gRPC.
- canonical language for config spec is Protobuf v3. There's a canonical Protobuf<->JSON(YAML) mapping [here](https://developers.google.com/protocol-buffers/docs/proto3#json) used to map cloud resources to protobuf messages.
- documentation should be generated from inline comment in .proto files
- root packages are:
  - **admin** Administration pourposes
  - **config** Configuration description
  - **service** Accessing services
