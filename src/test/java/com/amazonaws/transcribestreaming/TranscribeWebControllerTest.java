package com.amazonaws.transcribestreaming;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TranscribeWebControllerTest {

    @Mock
    private TranscribeStreamingClientWrapper clientWrapper;

    @Mock
    private TranscribeStreamingSynchronousClient synchronousClient;

    @InjectMocks
    private TranscribeWebController controller;

    @Test
    void testStartMicCallsWrapperWithVADListener() throws Exception {
        controller.startMic("en-US");

        // Capture the VAD listener passed to startTranscription
        ArgumentCaptor<Consumer<Double>> listenerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(clientWrapper).startTranscription(any(), eq(null), listenerCaptor.capture());

        Consumer<Double> listener = listenerCaptor.getValue();
        
        // Setup emitter to verify SSE send
        SseEmitter emitter = mock(SseEmitter.class);
        addEmitter(emitter);

        // Simulate VAD event
        listener.accept(0.1234);

        // Verify SSE message format
        verify(emitter).send("VAD:0.12");
    }

    @SuppressWarnings("unchecked")
    private void addEmitter(SseEmitter emitter) throws Exception {
        Field emittersField = TranscribeWebController.class.getDeclaredField("emitters");
        emittersField.setAccessible(true);
        List<SseEmitter> emitters = (List<SseEmitter>) emittersField.get(controller);
        emitters.add(emitter);
    }
}
