package com.amazonaws.transcribestreaming;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.services.transcribestreaming.model.AudioStream;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AudioStreamPublisherTest {

    @Mock
    private Subscriber<AudioStream> mockSubscriber;

    @Test
    void testSubscribe() {
        InputStream inputStream = new ByteArrayInputStream(new byte[10]);
        AudioStreamPublisher publisher = new AudioStreamPublisher(inputStream);

        publisher.subscribe(mockSubscriber);

        // Verify that onSubscribe is called on the subscriber
        verify(mockSubscriber).onSubscribe(any(Subscription.class));
    }

    @Test
    void testSubscribeWithAudioLevelListener() {
        InputStream inputStream = new ByteArrayInputStream(new byte[10]);
        Consumer<Double> mockListener = mock(Consumer.class);
        AudioStreamPublisher publisher = new AudioStreamPublisher(inputStream, mockListener);

        publisher.subscribe(mockSubscriber);

        // Verify that onSubscribe is called on the subscriber
        verify(mockSubscriber).onSubscribe(any(Subscription.class));
    }
}
