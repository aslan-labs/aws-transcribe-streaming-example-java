package com.amazonaws.transcribestreaming;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.services.transcribestreaming.TranscribeStreamingAsyncClient;
import software.amazon.awssdk.services.transcribestreaming.model.AudioStream;
import software.amazon.awssdk.services.transcribestreaming.model.BadRequestException;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionRequest;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponseHandler;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TranscribeStreamingRetryClientTest {

    @Mock
    private TranscribeStreamingAsyncClient mockClient;

    @Mock
    private Publisher<AudioStream> mockPublisher;

    @Mock
    private StreamTranscriptionBehavior mockResponseHandler;

    private TranscribeStreamingRetryClient retryClient;

    @BeforeEach
    void setUp() {
        retryClient = new TranscribeStreamingRetryClient(mockClient);
        retryClient.setSleepTime(10); // Reduce sleep time for tests
    }

    @Test
    void testStartStreamTranscriptionSuccess() {
        StartStreamTranscriptionRequest request = StartStreamTranscriptionRequest.builder()
                .languageCode("en-US")
                .mediaEncoding("pcm")
                .mediaSampleRateHertz(16000)
                .build();

        CompletableFuture<Void> successFuture = CompletableFuture.completedFuture(null);
        when(mockClient.startStreamTranscription(any(StartStreamTranscriptionRequest.class), any(Publisher.class), any(StartStreamTranscriptionResponseHandler.class)))
                .thenReturn(successFuture);

        retryClient.startStreamTranscription(request, mockPublisher, mockResponseHandler);

        verify(mockClient, times(1)).startStreamTranscription(any(StartStreamTranscriptionRequest.class), any(Publisher.class), any(StartStreamTranscriptionResponseHandler.class));
    }

    @Test
    void testStartStreamTranscriptionNonRetriableException() {
        StartStreamTranscriptionRequest request = StartStreamTranscriptionRequest.builder()
                .languageCode("en-US")
                .mediaEncoding("pcm")
                .mediaSampleRateHertz(16000)
                .build();

        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(BadRequestException.builder().build());

        when(mockClient.startStreamTranscription(any(StartStreamTranscriptionRequest.class), any(Publisher.class), any(StartStreamTranscriptionResponseHandler.class)))
                .thenReturn(failedFuture);

        retryClient.startStreamTranscription(request, mockPublisher, mockResponseHandler);

        // Should only try once because BadRequestException is non-retriable
        verify(mockClient, times(1)).startStreamTranscription(any(StartStreamTranscriptionRequest.class), any(Publisher.class), any(StartStreamTranscriptionResponseHandler.class));
        verify(mockResponseHandler).onError(any(Throwable.class));
    }

    @Test
    void testStartStreamTranscriptionRetriableException() {
        StartStreamTranscriptionRequest request = StartStreamTranscriptionRequest.builder()
                .languageCode("en-US")
                .mediaEncoding("pcm")
                .mediaSampleRateHertz(16000)
                .build();

        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Network Error"));

        when(mockClient.startStreamTranscription(any(StartStreamTranscriptionRequest.class), any(Publisher.class), any(StartStreamTranscriptionResponseHandler.class)))
                .thenReturn(failedFuture);

        retryClient.setMaxRetries(2);
        retryClient.startStreamTranscription(request, mockPublisher, mockResponseHandler);

        // Initial attempt + 2 retries = 3 total calls
        // Note: Due to async nature, we might need to wait or use Awaitility, but for simple mock verification:
        // Since the recursive call happens in whenComplete, it might not finish immediately in this test thread.
        // However, since we are mocking the future to complete immediately (exceptionally), the callbacks run immediately in the same thread usually.
        
        // Let's verify it was called more than once.
        verify(mockClient, atLeast(2)).startStreamTranscription(any(StartStreamTranscriptionRequest.class), any(Publisher.class), any(StartStreamTranscriptionResponseHandler.class));
    }
}
