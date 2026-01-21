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
import software.amazon.awssdk.services.transcribestreaming.model.Result;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponse;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptEvent;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptResultStream;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Uygulamanın giriş noktasıdır (Main Class).
 * Argüman verilirse CLI, verilmezse GUI olarak çalışır.
 */
public class TranscribeStreamingDemoApp {

    private static final Logger logger = LoggerFactory.getLogger(TranscribeStreamingDemoApp.class);

    public static void main(String[] args) {
        if (args.length == 0 && !GraphicsEnvironment.isHeadless()) {
            logger.info("Starting GUI...");
            TranscribeStreamingGui.main(args);
            return;
        }

        logger.info("TranscribeStreamingDemoApp starting in CLI mode...");

        TranscribeStreamingClientWrapper client = new TranscribeStreamingClientWrapper();

        if (args.length > 0 && !args[0].equals("--mic")) {
            // Dosya transkripsiyonu
            File inputFile = new File(args[0]);
            if (!inputFile.exists()) {
                logger.error("File not found: " + args[0]);
                return;
            }
            logger.info("Starting file transcription for: " + inputFile.getAbsolutePath());
            TranscribeStreamingSynchronousClient synchronousClient = new TranscribeStreamingSynchronousClient(TranscribeStreamingClientWrapper.getClient());
            String result = synchronousClient.transcribeFile(inputFile);
            System.out.println("\n--- Final Transcript ---");
            System.out.println(result);
            System.out.println("------------------------");
        } else {
            // Mikrofon transkripsiyonu
            logger.info("Starting microphone transcription... Press Ctrl+C to stop.");
            
            StreamTranscriptionBehavior behavior = new StreamTranscriptionBehavior() {
                @Override
                public void onError(Throwable e) {
                    logger.error("Error during streaming: ", e);
                }

                @Override
                public void onStream(TranscriptResultStream e) {
                    List<Result> results = ((TranscriptEvent) e).transcript().results();
                    if (results.size() > 0) {
                        Result firstResult = results.get(0);
                        if (firstResult.alternatives().size() > 0 &&
                                !firstResult.alternatives().get(0).transcript().isEmpty()) {
                            String transcript = firstResult.alternatives().get(0).transcript();
                            if (!firstResult.isPartial()) {
                                System.out.println("Final: " + transcript);
                            } else {
                                System.out.print("Partial: " + transcript + "\r");
                            }
                        }
                    }
                }

                @Override
                public void onResponse(StartStreamTranscriptionResponse r) {
                    logger.info("Received initial response from AWS Transcribe");
                }

                @Override
                public void onComplete() {
                    logger.info("Streaming completed");
                }
            };

            CompletableFuture<Void> streamingRequest = client.startTranscription(behavior, null);
            
            try {
                // CLI olduğu için süresiz bekle (veya kullanıcı Ctrl+C yapana kadar)
                streamingRequest.get();
            } catch (Exception e) {
                logger.error("Streaming request failed", e);
            }
        }

        client.close();
        logger.info("TranscribeStreamingDemoApp finished.");
    }
}
