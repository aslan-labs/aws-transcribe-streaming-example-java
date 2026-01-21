/*
 * Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.amazonaws.transcribestreaming;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.transcribestreaming.model.AudioStream;

import java.io.InputStream;
import java.util.function.Consumer;

/**
 * AudioStreamPublisher, ses verisi akışını yayınlayan (Publisher) sınıftır.
 * Reactive Streams standardına uygun olarak çalışır.
 *
 * Görevi:
 * Bir `InputStream` (örneğin mikrofon veya dosya) üzerinden gelen ham ses verisini alır
 * ve bunu `AudioStream` olayları olarak abonelere (Subscriber) iletir.
 * Bu işlem, AWS Transcribe Streaming servisine veri göndermek için kullanılır.
 *
 * AudioStreamPublisher implements audio stream publisher.
 * AudioStreamPublisher emits audio stream asynchronously in a separate thread
 */
public class AudioStreamPublisher implements Publisher<AudioStream> {

    private static final Logger logger = LoggerFactory.getLogger(AudioStreamPublisher.class);
    private final InputStream inputStream;
    private Consumer<Double> audioLevelListener;

    public AudioStreamPublisher(InputStream inputStream) {
        this(inputStream, null);
    }

    public AudioStreamPublisher(InputStream inputStream, Consumer<Double> audioLevelListener) {
        this.inputStream = inputStream;
        this.audioLevelListener = audioLevelListener;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public void subscribe(Subscriber<? super AudioStream> s) {
        logger.info("AudioStreamPublisher: New subscriber subscribed.");
        // Abonelik başladığında, ByteToAudioEventSubscription nesnesi oluşturulur.
        // Bu nesne, InputStream'den okunan verileri AudioEvent'lere dönüştürür.
        s.onSubscribe(new ByteToAudioEventSubscription(s, inputStream, audioLevelListener));
    }
}
