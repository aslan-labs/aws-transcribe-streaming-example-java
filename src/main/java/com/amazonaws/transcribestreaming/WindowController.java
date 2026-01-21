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

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.transcribestreaming.model.Result;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponse;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptEvent;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptResultStream;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Bu sınıf, uygulamanın Grafik Kullanıcı Arayüzünü (GUI) yönetir.
 * Kullanıcı etkileşimlerini (butonlara tıklama vb.) dinler ve ilgili transkripsiyon işlemlerini tetikler.
 *
 * Temel Sorumlulukları:
 * 1. JavaFX bileşenlerini (Butonlar, Metin Alanları) oluşturmak ve düzenlemek.
 * 2. Mikrofon veya dosya transkripsiyonunu başlatmak için `TranscribeStreamingClientWrapper` ve `TranscribeStreamingSynchronousClient` kullanmak.
 * 3. AWS Transcribe'dan gelen sonuçları ekranda göstermek.
 *
 * This class primarily controls the GUI for this application. Most of the code relevant to starting and working
 * with our streaming API can be found in TranscribeStreamingClientWrapper.java, with the exception of some result
 * parsing logic in this classes method getResponseHandlerForWindow()
 */
public class WindowController {

    private static final Logger logger = LoggerFactory.getLogger(WindowController.class);

    private TranscribeStreamingClientWrapper client;
    private TranscribeStreamingSynchronousClient synchronousClient;
    private TextArea outputTextArea;
    private Button startStopMicButton;
    private Button fileStreamButton;
    private Button saveButton;
    private TextArea finalTextArea;
    private CompletableFuture<Void> inProgressStreamingRequest;
    private String finalTranscript = "";
    private Stage primaryStage;

    public WindowController(Stage primaryStage) {
        logger.info("Initializing WindowController...");
        // İstemci sarmalayıcılarını başlat
        client = new TranscribeStreamingClientWrapper();
        synchronousClient = new TranscribeStreamingSynchronousClient(TranscribeStreamingClientWrapper.getClient());
        this.primaryStage = primaryStage;
        initializeWindow(primaryStage);
        logger.info("WindowController initialized.");
    }

    public void close() {
        logger.info("Closing WindowController resources...");
        // Uygulama kapanırken açık olan istekleri ve istemciyi kapat
        if (inProgressStreamingRequest != null) {
            inProgressStreamingRequest.completeExceptionally(new InterruptedException());
        }
        client.close();
    }

    /**
     * Dosyadan transkripsiyon isteğini başlatır.
     * Bu işlem senkron (bloklayan) olarak yapılır.
     */
    private void startFileTranscriptionRequest(File inputFile) {
        if (inProgressStreamingRequest == null) {
            logger.info("Starting file transcription request for file: {}", inputFile.getName());
            finalTextArea.clear();
            finalTranscript = "";
            startStopMicButton.setText("Streaming...");
            startStopMicButton.setDisable(true);
            outputTextArea.clear();
            finalTextArea.clear();
            saveButton.setDisable(true);
            
            // Senkron istemciyi kullanarak dosyayı işle
            finalTranscript = synchronousClient.transcribeFile(inputFile);
            
            finalTextArea.setText(finalTranscript);
            startStopMicButton.setDisable(false);
            saveButton.setDisable(false);
            startStopMicButton.setText("Start Microphone Transcription");
            logger.info("File transcription request completed.");
        }
    }

    /**
     * Mikrofon üzerinden canlı transkripsiyon isteğini başlatır.
     * Bu işlem asenkron olarak yapılır.
     */
    private void startTranscriptionRequest(File inputFile) {
        if (inProgressStreamingRequest == null) {
            logger.info("Starting microphone transcription request...");
            finalTextArea.clear();
            finalTranscript = "";
            startStopMicButton.setText("Connecting...");
            startStopMicButton.setDisable(true);
            outputTextArea.clear();
            finalTextArea.clear();
            saveButton.setDisable(true);
            
            // Asenkron istemciyi kullanarak akışı başlat
            inProgressStreamingRequest = client.startTranscription(getResponseHandlerForWindow(), inputFile);
        }
    }

    /**
     * JavaFX pencere bileşenlerini oluşturur ve yerleştirir.
     */
    private void initializeWindow(Stage primaryStage) {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setVgap(10);
        grid.setHgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Scene scene = new Scene(grid, 500, 600);
        primaryStage.setScene(scene);

        startStopMicButton = new Button();
        startStopMicButton.setText("Start Microphone Transcription");
        startStopMicButton.setOnAction(__ -> startTranscriptionRequest(null));
        grid.add(startStopMicButton, 0, 0, 1, 1);

        fileStreamButton = new Button();
        fileStreamButton.setText("Stream From Audio File"); //TODO: what file types do we support?
        fileStreamButton.setOnAction(__ -> {
            FileChooser inputFileChooser = new FileChooser();
            inputFileChooser.setTitle("Stream Audio File");
            File inputFile = inputFileChooser.showOpenDialog(primaryStage);
            if (inputFile != null) {
                startFileTranscriptionRequest(inputFile);
            }
        });
        grid.add(fileStreamButton, 1, 0, 1, 1);

        Text inProgressText = new Text("In Progress Transcriptions:");
        grid.add(inProgressText, 0, 1, 2, 1);

        outputTextArea = new TextArea();
        outputTextArea.setWrapText(true);
        outputTextArea.setEditable(false);
        grid.add(outputTextArea, 0, 2, 2, 1);

        Text finalText = new Text("Final Transcription:");
        grid.add(finalText, 0, 3, 2, 1);

        finalTextArea = new TextArea();
        finalTextArea.setWrapText(true);
        finalTextArea.setEditable(false);
        grid.add(finalTextArea, 0, 4, 2, 1);

        saveButton = new Button();
        saveButton.setDisable(true);
        saveButton.setText("Save Full Transcript");
        grid.add(saveButton, 0, 5, 2, 1);


    }

    /**
     * Devam eden transkripsiyon işlemini durdurur.
     */
    private void stopTranscription() {
        if (inProgressStreamingRequest != null) {
            try {
                logger.info("Stopping transcription...");
                saveButton.setDisable(true);
                client.stopTranscription();
                inProgressStreamingRequest.get(); // İşlemin tamamen bitmesini bekle
                logger.info("Transcription stopped successfully.");
            } catch (ExecutionException | InterruptedException e) {
                logger.error("Error closing stream: ", e);
            } finally {
                inProgressStreamingRequest = null;
                startStopMicButton.setText("Start Microphone Transcription");
                startStopMicButton.setOnAction(__ -> startTranscriptionRequest(null));
                startStopMicButton.setDisable(false);
            }

        }
    }

    /**
     * AWS Transcribe servisinden gelen olayları dinleyen ve işleyen bir `StreamTranscriptionBehavior` nesnesi döndürür.
     * Bu metod, gelen transkriptleri GUI'de göstermek ve sonuçları birleştirmekten sorumludur.
     *
     * A StartStreamTranscriptionResponseHandler class listens to events from Transcribe streaming service that return
     * transcriptions, and decides what to do with them. This example displays the transcripts in the GUI window, and
     * combines the transcripts together into a final transcript at the end.
     */
    private StreamTranscriptionBehavior getResponseHandlerForWindow() {
        return new StreamTranscriptionBehavior() {

            // AWS Transcribe'dan dönen hataları işler.
            // This will handle errors being returned from AWS Transcribe in your response. Here we just print the exception.
            @Override
            public void onError(Throwable e) {
                logger.error("Error received from Transcribe service: ", e);
                Throwable cause = e.getCause();
                while (cause != null) {
                    logger.error("Caused by: ", cause);
                    if (cause.getCause() != cause) { //Look out for circular causes
                        cause = cause.getCause();
                    } else {
                        cause = null;
                    }
                }
            }

            /*
            Transcribe servisinden gelen her bir olayı (event) işler.
            Gelen transkript parçalarını ekranda günceller ve "final" (kesinleşmiş) sonuçları ana metne ekler.
            
            This handles each event being received from the Transcribe service. In this example we are displaying the
            transcript as it is updated, and when we receive a "final" transcript, we append it to our finalTranscript
            which is returned at the end of the microphone streaming.
             */
            @Override
            public void onStream(TranscriptResultStream event) {
                List<Result> results = ((TranscriptEvent) event).transcript().results();
                if(results.size()>0) {
                    Result firstResult = results.get(0);
                    if (firstResult.alternatives().size() > 0 && !firstResult.alternatives().get(0).transcript().isEmpty()) {
                        String transcript = firstResult.alternatives().get(0).transcript();
                        if(!transcript.isEmpty()) {
                            logger.info("Transcript received: {}", transcript);
                            String displayText;
                            if (!firstResult.isPartial()) {
                                // Sonuç kesinleştiyse (partial değilse), ana metne ekle
                                finalTranscript += transcript + " ";
                                displayText = finalTranscript;
                            } else {
                                // Sonuç henüz kesinleşmediyse, geçici olarak göster
                                displayText = finalTranscript + " " + transcript;
                            }
                            // UI güncellemeleri JavaFX Application Thread üzerinde yapılmalı
                            Platform.runLater(() -> {
                                outputTextArea.setText(displayText);
                                outputTextArea.setScrollTop(Double.MAX_VALUE);
                            });
                        }
                    }

                }
            }

            /*
            AWS Transcribe servisinden ilk yanıt alındığında çağrılır.
            Bağlantının başarılı olduğunu gösterir. UI'da butonu "Durdur" moduna geçirir.
            
            This handles the initial response from the AWS Transcribe service, generally indicating the streams have
            successfully been opened. Here we just print that we have received the initial response and do some
            UI updates.
             */
            @Override
            public void onResponse(StartStreamTranscriptionResponse r) {
                logger.info("Received Initial response. Request Id: {}", r.requestId());
                Platform.runLater(() -> {
                    startStopMicButton.setText("Stop Transcription");
                    startStopMicButton.setOnAction(__ -> stopTranscription());
                    startStopMicButton.setDisable(false);
                });
            }

            /*
            Akış hatasız bir şekilde sonlandığında çağrılır.
            Nihai transkripti gösterir ve kaydetme butonunu aktif eder.

            This method is called when the stream is terminated without error. In our case we will use this opportunity
            to display the final, total transcript we've been aggregating during the transcription period and activates
            the save button.
             */
            @Override
            public void onComplete() {
                logger.info("Transcription stream completed successfully.");
                Platform.runLater(() -> {
                    finalTextArea.setText(finalTranscript);
                    saveButton.setDisable(false);
                    saveButton.setOnAction(__ -> {
                        FileChooser fileChooser = new FileChooser();
                        fileChooser.setTitle("Save Transcript");
                        File file = fileChooser.showSaveDialog(primaryStage);
                        if (file != null) {
                            try {
                                FileWriter writer = new FileWriter(file);
                                writer.write(finalTranscript);
                                writer.close();
                                logger.info("Transcript saved to file: {}", file.getAbsolutePath());
                            } catch (IOException e) {
                                logger.error("Error saving transcript to file: ", e);
                            }
                        }
                    });

                });
            }
        };
    }

}
