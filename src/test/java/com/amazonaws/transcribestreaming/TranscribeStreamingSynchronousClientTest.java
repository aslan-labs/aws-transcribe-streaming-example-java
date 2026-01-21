package com.amazonaws.transcribestreaming;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.transcribestreaming.TranscribeStreamingAsyncClient;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionRequest;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponseHandler;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranscribeStreamingSynchronousClientTest {

    @Mock
    private TranscribeStreamingAsyncClient mockAsyncClient;

    @Test
    void testTranscribeFileThrowsExceptionForInvalidFile() {
        TranscribeStreamingSynchronousClient client = new TranscribeStreamingSynchronousClient(mockAsyncClient);
        File invalidFile = new File("non_existent_file.wav");

        assertThrows(RuntimeException.class, () -> {
            client.transcribeFile(invalidFile);
        });
    }

    // Note: Testing successful transcription requires mocking AudioSystem which is a static system class.
    // This is hard to do without PowerMock or similar.
    // We will skip the success case for now as it involves reading a real audio file.
}
