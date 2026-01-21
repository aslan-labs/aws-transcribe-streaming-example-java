package com.amazonaws.transcribestreaming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import software.amazon.awssdk.services.transcribestreaming.model.LanguageCode;
import software.amazon.awssdk.services.transcribestreaming.model.Result;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponse;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptEvent;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptResultStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

@Controller
public class TranscribeWebController {

    private static final Logger logger = LoggerFactory.getLogger(TranscribeWebController.class);

    @Autowired
    private TranscribeStreamingClientWrapper clientWrapper;

    @Autowired
    private TranscribeStreamingSynchronousClient synchronousClient;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/api/transcribe/start-mic")
    @ResponseBody
    public void startMic(@RequestParam(defaultValue = "en-US") String language) {
        clientWrapper.setLanguageCode(LanguageCode.fromValue(language));
        
        StreamTranscriptionBehavior behavior = new StreamTranscriptionBehavior() {
            @Override
            public void onError(Throwable e) {
                logger.error("Error: ", e);
                sendToEmitters("HATA: " + e.getMessage());
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
                            sendToEmitters("TEXT:" + transcript);
                        }
                    }
                }
            }

            @Override
            public void onResponse(StartStreamTranscriptionResponse r) {
                logger.info("AWS Connection established");
            }

            @Override
            public void onComplete() {
                logger.info("Transcription complete");
            }
        };

        clientWrapper.startTranscription(behavior, null, level -> {
            sendToEmitters("VAD:" + String.format(Locale.US, "%.2f", level));
        });
    }

    @PostMapping("/api/transcribe/stop-mic")
    @ResponseBody
    public void stopMic() {
        clientWrapper.stopTranscription();
    }

    @PostMapping("/api/transcribe/file")
    @ResponseBody
    public String transcribeFile(@RequestParam("file") MultipartFile file, 
                                 @RequestParam(defaultValue = "en-US") String language) throws IOException {
        File tempFile = File.createTempFile("upload", file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(file.getBytes());
        }
        
        try {
            return synchronousClient.transcribeFile(tempFile, LanguageCode.fromValue(language));
        } finally {
            tempFile.delete();
        }
    }

    @GetMapping("/api/transcribe/events")
    public SseEmitter listenToEvents() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        this.emitters.add(emitter);
        emitter.onCompletion(() -> this.emitters.remove(emitter));
        emitter.onTimeout(() -> this.emitters.remove(emitter));
        return emitter;
    }

    private void sendToEmitters(String text) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(text);
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
}