package japlearn.demo.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

@Service
public class GuidedPhrasePronunciationService {
    public record Scores(String recognizedText,double pronunciation,double accuracy,double fluency,double completeness){}
    @Value("${azure.speech.key:}") private String key;
    @Value("${azure.speech.region:southeastasia}") private String region;
    public boolean configured(){return key!=null&&!key.isBlank();}
    public Scores assess(MultipartFile upload,String reference) throws Exception{
        if(!configured())throw new IllegalStateException("Azure Speech is not configured.");
        if(upload==null||upload.isEmpty())throw new IllegalArgumentException("A spoken response is required.");
        if(reference==null||reference.isBlank())throw new IllegalArgumentException("The current practice phrase is missing.");
        Path input=Files.createTempFile("guided-input-",".audio"),wav=Files.createTempFile("guided-azure-",".wav");
        try{upload.transferTo(input);convert(input,wav);return sdk(wav,reference);}finally{Files.deleteIfExists(input);Files.deleteIfExists(wav);}
    }
    private Scores sdk(Path wav,String reference)throws Exception{
        try(SpeechConfig speech=SpeechConfig.fromSubscription(key,region)){
            speech.setSpeechRecognitionLanguage("ja-JP");
            try(AudioConfig audio=AudioConfig.fromWavFileInput(wav.toString());SpeechRecognizer recognizer=new SpeechRecognizer(speech,audio);
                PronunciationAssessmentConfig config=new PronunciationAssessmentConfig(reference,PronunciationAssessmentGradingSystem.HundredMark,PronunciationAssessmentGranularity.Phoneme,true)){
                config.applyTo(recognizer);
                try(SpeechRecognitionResult result=recognizer.recognizeOnceAsync().get(35,TimeUnit.SECONDS)){
                    if(result.getReason()!=ResultReason.RecognizedSpeech)throw new IOException("Azure could not recognize the response.");
                    PronunciationAssessmentResult score=PronunciationAssessmentResult.fromResult(result);
                    return new Scores(result.getText(),round(score.getPronunciationScore()),round(score.getAccuracyScore()),round(score.getFluencyScore()),round(score.getCompletenessScore()));
                }
            }
        }
    }
    private double round(double n){return Math.round(n*10.0)/10.0;}
    private void convert(Path input,Path output)throws Exception{Process p=new ProcessBuilder("ffmpeg","-nostdin","-hide_banner","-loglevel","error","-y","-i",input.toString(),"-ac","1","-ar","16000","-c:a","pcm_s16le",output.toString()).start();if(!p.waitFor(30,TimeUnit.SECONDS)){p.destroyForcibly();throw new IOException("Audio conversion timed out.");}if(p.exitValue()!=0||Files.size(output)<=44)throw new IOException("The recording could not be processed.");}
}
