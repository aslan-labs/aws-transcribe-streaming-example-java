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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.transcribestreaming.TranscribeStreamingAsyncClient;
import software.amazon.awssdk.services.transcribestreaming.model.LanguageCode;
import software.amazon.awssdk.services.transcribestreaming.model.MediaEncoding;
import software.amazon.awssdk.services.transcribestreaming.model.Result;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionRequest;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponseHandler;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptEvent;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Asenkron (Async) çalışan AWS Transcribe istemcisini, senkron (bloklayan) bir şekilde kullanmak için tasarlanmış sarmalayıcı sınıftır.
 *
 * Kullanım Amacı:
 * Özellikle dosya tabanlı transkripsiyonlarda, tüm dosyanın işlenmesi bitene kadar beklemek istendiğinde kullanılır.
 * `CompletableFuture.get()` metodunu kullanarak işlem bitene kadar ana akışı bekletir.
 *
 * An example implementation of a simple synchronous wrapper around the async client
 */
public class TranscribeStreamingSynchronousClient {

    private static final Logger logger = LoggerFactory.getLogger(TranscribeStreamingSynchronousClient.class);
    public static final int MAX_TIMEOUT_MS = 15 * 60 * 1000; //15 minutes

    private TranscribeStreamingAsyncClient asyncClient;
    private String finalTranscript = "";

    public TranscribeStreamingSynchronousClient(TranscribeStreamingAsyncClient asyncClient) {
        this.asyncClient = asyncClient;
    }

    /**
     * Verilen ses dosyasını senkron olarak transkribe eder.
     * İşlem bitene kadar (veya timeout olana kadar) bekler.
     */
    public String transcribeFile(File audioFile) {
        return transcribeFile(audioFile, LanguageCode.EN_US);
    }

    /**
     * Verilen ses dosyas??n?? senkron olarak transkribe eder.
     * ????lem bitene kadar (veya timeout olana kadar) bekler.
     */
    public String transcribeFile(File audioFile, LanguageCode languageCode) {
        try {
            int sampleRate = (int) AudioSystem.getAudioInputStream(audioFile).getFormat().getSampleRate();
            StartStreamTranscriptionRequest request = buildRequest(sampleRate, languageCode);
            AudioStreamPublisher audioStream = new AudioStreamPublisher(new FileInputStream(audioFile));
            StartStreamTranscriptionResponseHandler responseHandler = getResponseHandler();
            logger.info("launching request");
            CompletableFuture<Void> resultFuture = asyncClient.startStreamTranscription(request, audioStream, responseHandler);
            logger.info("waiting for response, this will take some time depending on the length of the audio file");
            resultFuture.get(MAX_TIMEOUT_MS, TimeUnit.MILLISECONDS); //block until done
        } catch (IOException e) {
            logger.error("Error reading audio file ({}): ", audioFile.getName(), e);
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            logger.error("Error streaming audio to AWS Transcribe service: ", e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            logger.error("Stream thread interupted: ", e);
            throw new RuntimeException(e);
        } catch (UnsupportedAudioFileException e) {
            logger.error("File type not recognized: {}, error: ", audioFile.getName(), e);
        } catch (TimeoutException e) {
            logger.error("Stream not closed within timeout window of {} ms", MAX_TIMEOUT_MS, e);
            throw new RuntimeException(e);
        }
        return finalTranscript;
    }

    StartStreamTranscriptionRequest buildRequest(int sampleRate, LanguageCode languageCode) {
        LanguageCode effectiveLanguage = (languageCode == null) ? LanguageCode.EN_US : languageCode;
        return StartStreamTranscriptionRequest.builder()
                .languageCode(effectiveLanguage.toString())
                .mediaEncoding(MediaEncoding.PCM)
                .mediaSampleRateHertz(sampleRate)
                .build();
    }

    /**
     * Gelen transkriptleri biriktiren bir yanıt işleyici (Response Handler) döndürür.
     * Get a response handler that aggregates the transcripts as they arrive
     * @return Response handler used to handle events from AWS Transcribe service.
     */
    private StartStreamTranscriptionResponseHandler getResponseHandler() {
        return StartStreamTranscriptionResponseHandler.builder()
                .subscriber(event -> {
                    List<Result> results = ((TranscriptEvent) event).transcript().results();
                    if(results.size()>0) {
                        Result firstResult = results.get(0);
                        if (firstResult.alternatives().size() > 0 &&
                                !firstResult.alternatives().get(0).transcript().isEmpty()) {
                            String transcript = firstResult.alternatives().get(0).transcript();
                            if(!transcript.isEmpty() && !firstResult.isPartial()) {
                                logger.info(transcript);
                                finalTranscript += transcript;
                            }
                        }

                    }
                }).build();
    }


}
