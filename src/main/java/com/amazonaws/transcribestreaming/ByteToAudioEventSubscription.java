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

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.transcribestreaming.model.AudioEvent;
import software.amazon.awssdk.services.transcribestreaming.model.AudioStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Bu sınıf, bir `InputStream`'den okunan byte verilerini AWS Transcribe servisinin beklediği
 * `AudioEvent` nesnelerine dönüştüren bir Reactive Streams `Subscription` uygulamasıdır.
 *
 * Çalışma Mantığı:
 * 1. `Publisher` (AudioStreamPublisher) ile `Subscriber` (AWS SDK Client) arasındaki bağlantıyı temsil eder.
 * 2. `Subscriber` veri talep ettiğinde (`request` metodu), `InputStream`'den belirli bir boyutta (CHUNK_SIZE) veri okur.
 * 3. Okunan veriyi `AudioEvent` içine paketler ve `subscriber.onNext()` ile gönderir.
 * 4. Veri bitene kadar veya talep karşılanana kadar bu işlemi tekrarlar.
 *
 * Bu yapı, "Backpressure" (Geri Basınç) yönetimini sağlar; yani tüketici (AWS) hazır oldukça veri gönderilir.
 *
 * This is an example Subscription implementation that converts bytes read from an AudioStream into AudioEvents
 * that can be sent to the Transcribe service. It implements a simple demand system that will read chunks of bytes
 * from an input stream containing audio data
 *
 * To read more about how Subscriptions and reactive streams work, please see
 * https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.2/README.md
 */
public class ByteToAudioEventSubscription implements Subscription {
    private static final Logger logger = LoggerFactory.getLogger(ByteToAudioEventSubscription.class);
    private static final int CHUNK_SIZE_IN_BYTES = 1024 * 4;
    private ExecutorService executor = Executors.newFixedThreadPool(1);
    private AtomicLong demand = new AtomicLong(0);

    private final Subscriber<? super AudioStream> subscriber;
    private final InputStream inputStream;
    private Consumer<Double> audioLevelListener;

    public ByteToAudioEventSubscription(Subscriber<? super AudioStream> s, InputStream inputStream) {
        this(s, inputStream, null);
    }

    public ByteToAudioEventSubscription(Subscriber<? super AudioStream> s, InputStream inputStream, Consumer<Double> audioLevelListener) {
        this.subscriber = s;
        this.inputStream = inputStream;
        this.audioLevelListener = audioLevelListener;
    }

    @Override
    public void request(long n) {
        if (n <= 0) {
            subscriber.onError(new IllegalArgumentException("Demand must be positive"));
        }

        demand.getAndAdd(n);
        //We need to invoke this in a separate thread because the call to subscriber.onNext(...) is recursive
        // Veri okuma ve gönderme işlemini ayrı bir thread'de yapıyoruz çünkü onNext çağrısı recursive olabilir.
        executor.submit(() -> {
            try {
                do {
                    ByteBuffer audioBuffer = getNextEvent();
                    if (audioBuffer.remaining() > 0) {
                        calculateAudioLevel(audioBuffer.duplicate());
                        AudioEvent audioEvent = audioEventFromBuffer(audioBuffer);
                        subscriber.onNext(audioEvent);
                    } else {
                        logger.info("ByteToAudioEventSubscription: Stream completed.");
                        subscriber.onComplete();
                        break;
                    }
                } while (demand.decrementAndGet() > 0);
            } catch (Exception e) {
                logger.error("ByteToAudioEventSubscription: Error processing stream: ", e);
                subscriber.onError(e);
            }
        });
    }

    @Override
    public void cancel() {
        logger.info("ByteToAudioEventSubscription: Cancelled.");
        executor.shutdown();
    }

    /**
     * InputStream'den bir sonraki veri parçasını (chunk) okur.
     */
    private ByteBuffer getNextEvent() {
        ByteBuffer audioBuffer = null;
        byte[] audioBytes = new byte[CHUNK_SIZE_IN_BYTES];

        try {
            int len = inputStream.read(audioBytes);

            if (len <= 0) {
                audioBuffer = ByteBuffer.allocate(0);
            } else {
                audioBuffer = ByteBuffer.wrap(audioBytes, 0, len);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return audioBuffer;
    }

    /**
     * Ses seviyesini (RMS) hesaplar ve dinleyiciye bildirir.
     */
    private void calculateAudioLevel(ByteBuffer bb) {
        if (audioLevelListener == null) return;

        // Varsayılan: 16-bit PCM, Little Endian
        bb.order(ByteOrder.LITTLE_ENDIAN);
        double sum = 0;
        int count = 0;

        while (bb.remaining() >= 2) {
            short sample = bb.getShort();
            sum += sample * sample;
            count++;
        }

        if (count > 0) {
            double rms = Math.sqrt(sum / count);
            // Normalizasyon: 0.0 ile 1.0 arası (32768 max short değeri)
            double normalized = Math.min(1.0, rms / 32768.0);
            audioLevelListener.accept(normalized);
        }
    }

    /**
     * Okunan byte verisini AWS AudioEvent nesnesine dönüştürür.
     */
    private AudioEvent audioEventFromBuffer(ByteBuffer bb) {
        return AudioEvent.builder()
                .audioChunk(SdkBytes.fromByteBuffer(bb))
                .build();
    }
}
