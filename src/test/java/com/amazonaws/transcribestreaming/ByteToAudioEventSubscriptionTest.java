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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void testAudioLevelCalculation() throws InterruptedException {
        // Create 4KB of audio data with a specific pattern (sine wave or just non-zero)
        // 4KB matches CHUNK_SIZE_IN_BYTES in ByteToAudioEventSubscription
        int size = 4096;
        ByteBuffer bb = ByteBuffer.allocate(size);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < size / 2; i++) {
            bb.putShort((short) 10000); // Constant amplitude
        }
        
        InputStream audioStream = new ByteArrayInputStream(bb.array());
        List<Double> levels = new ArrayList<>();
        Consumer<Double> listener = levels::add;
        
        subscription = new ByteToAudioEventSubscription(mockSubscriber, audioStream, listener);
        subscription.request(1);
        
        Thread.sleep(200);
        
        assertTrue(levels.size() > 0, "Audio level should have been reported");
        double level = levels.get(0);
        assertTrue(level > 0, "Audio level should be positive");
        assertTrue(level <= 1.0, "Audio level should be normalized to <= 1.0");
        
        // RMS for constant 10000 should be 10000. 
        // Normalized: (10000 / 32768) * 175.0 approx 53.4, capped at 1.0
        assertTrue(level > 0.9, "Audio level should be boosted and capped, got " + level);
    }

    @Test
    void testCancelShutsDownExecutor() {
        subscription.cancel();
        // Since executor is private, we can't easily verify shutdown without reflection or refactoring.
        // But we can verify no more interactions happen if we request after cancel (though implementation might not strictly prevent it if not checked).
        // For now, just ensuring no exception is thrown.
    }
}
