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

import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uygulamanın giriş noktasıdır (Main Class).
 * JavaFX uygulamasını başlatır ve ana pencere kontrolcüsünü (WindowController) yükler.
 */
public class TranscribeStreamingDemoApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(TranscribeStreamingDemoApp.class);

    @Override
    public void start(Stage primaryStage)  {
        logger.info("TranscribeStreamingDemoApp starting...");

        // Ana pencere kontrolcüsünü oluştur
        WindowController windowController = new WindowController(primaryStage);

        // Pencere kapatıldığında uygulamayı ve kaynakları temizle
        primaryStage.setOnCloseRequest(__ -> {
            logger.info("Application closing...");
            windowController.close();
            System.exit(0);
        });
        primaryStage.show();
        logger.info("TranscribeStreamingDemoApp started and window shown.");

    }

    public static void main(String args[]) {
        launch(args);
    }

}
