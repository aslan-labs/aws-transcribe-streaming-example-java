# AWS Transcribe Streaming Example Java Application 

Example Java Application using AWS SDK creating streaming transcriptions via AWS Transcribe (CLI version).

## License Summary

This sample code is made available under a modified MIT license. See the LICENSE file.

## Setup

This application builds with Java 8 or higher.

This application assumes your credentials are defined in the same way the [Default Credential Provider Chain](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html#credentials-default)
requires.

There is a new permission required to use streaming transcription, StartStreamTranscription. You can use a policy like this:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "transcribestreaming",
            "Effect": "Allow",
            "Action": "transcribe:StartStreamTranscription",
            "Resource": "*"
        }
    ]
}
```

To generate an executable jar, use the following commands:
```bash
mvn clean package
```

To run the application (GUI mode):
```bash
java -jar target/aws-transcribe-sample-application-1.0-SNAPSHOT.jar
```

To run the application for microphone transcription (CLI mode):
```bash
java -jar target/aws-transcribe-sample-application-1.0-SNAPSHOT.jar --mic
```

To run the application for file transcription (CLI mode):
```bash
java -jar target/aws-transcribe-sample-application-1.0-SNAPSHOT.jar <path-to-audio-file>
```

## Description

This application demonstrates how to use AWS Transcribe's streaming API via both a graphical user interface (Swing) and a command-line interface.
The code with the call to the Transcribe API is located in TranscribeStreamingClientWrapper.java, in the 
"startTranscription" method.

This API takes advantage of a more advanced AWS SDK feature: the EventStream. These allow for streaming APIs by defining
behaviors to execute for multiple types of events, including success and error events. You can see an example 
implementation of this behavior defined in the TranscribeStreamingDemoApp.java class.
These events are handled asynchronously, but you can see an example of treating the streaming API as a synchronous 
service in the TranscribeStreamingSynchronousClient.java class, which is used for reading files.

## Classes

|Class|Description|
|---|---|
| `TranscribeStreamingDemoApp` | Main method that launches the application (GUI or CLI) |
| `TranscribeStreamingGui` | Simple Swing-based Graphical User Interface |
| `TranscribeStreamingClientWrapper` | Wrapper around the AWS SDK Transcribe Client, provides examples of how to call the SDK's methods properly |
| `AudioStreamPublisher` | Used to provide streaming events to the service, wraps `ByteToAudioEventSubscription` |
| `ByteToAudioEventSubscription` | Converts bytes from audio input into AudioEvents to send to the AWS Transcribe Service |
| `TranscribeStreamingRetryClient` | Wraps retry logic around the AWS Transcribe SDK, including resuming sessions in the case of disconnects |
| `StreamTranscriptionBehavior` | Interface to determine response handling behavior |
| `TranscribeStreamingSynchronousClient` | Class providing example of turning the asynchronous event-stream API into a synchronous one | 

## See Also
https://docs.aws.amazon.com/transcribe/latest/dg/streaming.html
