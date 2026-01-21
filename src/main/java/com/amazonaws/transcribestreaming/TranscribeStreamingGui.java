package com.amazonaws.transcribestreaming;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.transcribestreaming.model.Result;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponse;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptEvent;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptResultStream;
import software.amazon.awssdk.services.transcribestreaming.model.LanguageCode;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * AWS Transcribe için basit bir Swing tabanlı arayüz.
 */
@Component
public class TranscribeStreamingGui extends JFrame {

    private JTextArea textArea;
    private JButton micButton;
    private JButton fileButton;
    private JButton stopButton;
    private JComboBox<LanguageOption> languageCombo;
    
    @Autowired
    private TranscribeStreamingClientWrapper clientWrapper;
    
    @Autowired
    private TranscribeStreamingSynchronousClient synchronousClient;

    private CompletableFuture<Void> streamingRequest;

    public TranscribeStreamingGui() {
        initGui();
    }

    private void initGui() {
        setTitle("AWS Transcribe Streaming Demo");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        micButton = new JButton("Mikrofon Başlat");
        fileButton = new JButton("Dosya Seç ve Transkribe Et");
        stopButton = new JButton("Durdur");
        stopButton.setEnabled(false);

        languageCombo = new JComboBox<>(new LanguageOption[]{
                new LanguageOption("English (en-US)", LanguageCode.EN_US),
                new LanguageOption("Turkce (tr-TR)", LanguageCode.TR_TR)
        });
        languageCombo.setSelectedIndex(0);

        buttonPanel.add(new JLabel("Dil:"));
        buttonPanel.add(languageCombo);
        buttonPanel.add(micButton);
        buttonPanel.add(fileButton);
        buttonPanel.add(stopButton);
        add(buttonPanel, BorderLayout.SOUTH);

        micButton.addActionListener(e -> startMicTranscription());
        fileButton.addActionListener(e -> startFileTranscription());
        stopButton.addActionListener(e -> stopTranscription());

        setLocationRelativeTo(null);
    }

    private void startMicTranscription() {
        textArea.setText("Mikrofon başlatılıyor...\n");
        micButton.setEnabled(false);
        fileButton.setEnabled(false);
        stopButton.setEnabled(true);

        clientWrapper.setLanguageCode(getSelectedLanguageCode());
        StreamTranscriptionBehavior behavior = new StreamTranscriptionBehavior() {
            @Override
            public void onError(Throwable e) {
                SwingUtilities.invokeLater(() -> {
                    textArea.append("\nHata: " + e.getMessage() + "\n");
                    resetButtons();
                });
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
                            SwingUtilities.invokeLater(() -> textArea.append(transcript + " "));
                        }
                    }
                }
            }

            @Override
            public void onResponse(StartStreamTranscriptionResponse r) {
                SwingUtilities.invokeLater(() -> textArea.append("AWS Bağlantısı Kuruldu.\n"));
            }

            @Override
            public void onComplete() {
                SwingUtilities.invokeLater(() -> {
                    textArea.append("\nİşlem Tamamlandı.\n");
                    resetButtons();
                });
            }
        };

        streamingRequest = clientWrapper.startTranscription(behavior, null);
    }

    private void startFileTranscription() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            textArea.setText("Dosya işleniyor: " + selectedFile.getName() + "\n");
            micButton.setEnabled(false);
            fileButton.setEnabled(false);

            new Thread(() -> {
                try {
                    LanguageCode selectedLanguage = getSelectedLanguageCode();
                    String transcript = synchronousClient.transcribeFile(selectedFile, selectedLanguage);
                    SwingUtilities.invokeLater(() -> {
                        textArea.append("\n--- Transkript ---\n");
                        textArea.append(transcript);
                        textArea.append("\n------------------\n");
                        resetButtons();
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        textArea.append("\nHata: " + ex.getMessage() + "\n");
                        resetButtons();
                    });
                }
            }).start();
        }
    }

    private void stopTranscription() {
        clientWrapper.stopTranscription();
        if (streamingRequest != null) {
            streamingRequest.cancel(true);
        }
        resetButtons();
    }

    private void resetButtons() {
        micButton.setEnabled(true);
        fileButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    private LanguageCode getSelectedLanguageCode() {
        LanguageOption selected = (LanguageOption) languageCombo.getSelectedItem();
        return (selected == null) ? LanguageCode.EN_US : selected.code;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new TranscribeStreamingGui().setVisible(true);
        });
    }
    private static class LanguageOption {
        private final String label;
        private final LanguageCode code;

        private LanguageOption(String label, LanguageCode code) {
            this.label = label;
            this.code = code;
        }

        @Override
        public String toString() {
            return label;
        }
    }

}