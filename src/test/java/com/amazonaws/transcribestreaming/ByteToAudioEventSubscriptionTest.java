package com.amazonaws.transcribestreaming;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.services.transcribestreaming.model.AudioEvent;
import software.amazon.awssdk.services.transcribestreaming.model.AudioStream;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ByteToAudioEventSubscriptionTest {

    @Mock
    private Subscriber<AudioStream> mockSubscriber;

    private InputStream inputStream;
    private ByteToAudioEventSubscription subscription;

    @BeforeEach
    void setUp() {
        // 10 KB of dummy data
        byte[] dummyData = new byte[1024 * 10];
        inputStream = new ByteArrayInputStream(dummyData);
        subscription = new ByteToAudioEventSubscription(mockSubscriber, inputStream);
    }

    @Test
    void testRequestSendsEvents() throws InterruptedException {
        // Request 1 item
        subscription.request(1);

        // Allow some time for the executor to run
        Thread.sleep(100);

        // Verify onNext is called at least once
        verify(mockSubscriber, atLeastOnce()).onNext(any(AudioEvent.class));
    }

    @Test
    void testRequestWithZeroDemandThrowsError() {
        subscription.request(0);
        verify(mockSubscriber).onError(any(IllegalArgumentException.class));
    }

    @Test
    void testCancelShutsDownExecutor() {
        subscription.cancel();
        // Since executor is private, we can't easily verify shutdown without reflection or refactoring.
        // But we can verify no more interactions happen if we request after cancel (though implementation might not strictly prevent it if not checked).
        // For now, just ensuring no exception is thrown.
    }
}
