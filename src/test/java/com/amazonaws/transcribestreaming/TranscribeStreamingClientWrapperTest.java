package com.amazonaws.transcribestreaming;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.transcribestreaming.model.LanguageCode;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionRequest;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class TranscribeStreamingClientWrapperTest {

    @Test
    void testStartTranscriptionWithInvalidFile() {
        TranscribeStreamingClientWrapper wrapper = new TranscribeStreamingClientWrapper();
        File invalidFile = new File("invalid.wav");
        
        // Since we can't easily mock the internal client without refactoring, 
        // we expect this to fail either at file reading or client connection.
        // Given the implementation, it tries to read AudioSystem first.
        
        // This test mainly ensures no unexpected runtime crashes occur before the logical checks.
        // However, AudioSystem.getAudioInputStream will throw IOException which is caught and returns a failed future.
        
        CompletableFuture<Void> future = wrapper.startTranscription(null, invalidFile);
        assertThrows(Exception.class, future::join);
    }
    
    @Test
    void testStopTranscriptionSafeToCallWhenNotStarted() {
        TranscribeStreamingClientWrapper wrapper = new TranscribeStreamingClientWrapper();
        // Should not throw exception
        wrapper.stopTranscription();
    }

    @Test
    void testBuildRequestUsesLanguageCode() {
        TranscribeStreamingClientWrapper wrapper = new TranscribeStreamingClientWrapper();
        StartStreamTranscriptionRequest request = wrapper.buildRequest(16000, LanguageCode.TR_TR);

        assertEquals(LanguageCode.TR_TR, request.languageCode());
    }
}
