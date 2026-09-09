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
    public record Scores(String recognizedText,String referenceText,double pronunciation,double accuracy,double fluency,double completeness,String clarityLevel,String recommendation,String practiceTip,String sampleResponse,boolean speechDetected){}
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
    public Scores assessOpenConversation(MultipartFile upload) throws Exception{
        if(!configured())throw new IllegalStateException("Azure Speech is not configured.");
        if(upload==null||upload.isEmpty())throw new IllegalArgumentException("A spoken response is required.");
        Path input=Files.createTempFile("conversation-input-",".audio"),wav=Files.createTempFile("conversation-azure-",".wav");
        try{upload.transferTo(input);convert(input,wav);return sdk(wav,"");}finally{Files.deleteIfExists(input);Files.deleteIfExists(wav);}
    }
    private Scores sdk(Path wav,String reference)throws Exception{
        try(SpeechConfig speech=SpeechConfig.fromSubscription(key,region)){
            speech.setSpeechRecognitionLanguage("ja-JP");
            try(AudioConfig audio=AudioConfig.fromWavFileInput(wav.toString());SpeechRecognizer recognizer=new SpeechRecognizer(speech,audio);
                PronunciationAssessmentConfig config=new PronunciationAssessmentConfig(reference==null?"":reference,PronunciationAssessmentGradingSystem.HundredMark,PronunciationAssessmentGranularity.Phoneme,reference!=null&&!reference.isBlank())){
                config.applyTo(recognizer);
                try(SpeechRecognitionResult result=recognizer.recognizeOnceAsync().get(35,TimeUnit.SECONDS)){
                    if(result.getReason()==ResultReason.NoMatch)return noSpeech(reference);
                    if(result.getReason()!=ResultReason.RecognizedSpeech)throw new IOException("Azure could not recognize the response.");
                    PronunciationAssessmentResult score=PronunciationAssessmentResult.fromResult(result);
                    String recognized=cleanRecognizedText(result.getText());
                    if(recognized.isBlank())return noSpeech(reference);
                    double pronunciation=round(score.getPronunciationScore()),accuracy=round(score.getAccuracyScore()),fluency=round(score.getFluencyScore()),completeness=round(score.getCompletenessScore());
                    return new Scores(recognized,reference,pronunciation,accuracy,fluency,completeness,clarity(pronunciation),recommendation(pronunciation,accuracy,fluency,completeness),practiceTip(pronunciation,accuracy,fluency,completeness),reference,true);
                }
            }
        }
    }
    private double round(double n){return Math.round(n*10.0)/10.0;}
    private Scores noSpeech(String reference){return new Scores("",reference,0,0,0,0,"We didn't hear a phrase","No spoken phrase was detected, so this attempt was not scored.","Take a breath, move a little closer to the microphone, and speak when you feel ready.",reference,false);}
    private String cleanRecognizedText(String text){if(text==null)return "";String cleaned=text.trim();return cleaned.codePoints().anyMatch(Character::isLetterOrDigit)?cleaned:"";}
    private String clarity(double score){if(score>=85)return "Clear pronunciation";if(score>=70)return "Understandable—keep refining";return "Needs another careful try";}
    private String recommendation(double pronunciation,double accuracy,double fluency,double completeness){if(completeness<70)return "Say the complete model phrase, including its final particles and polite ending.";if(accuracy<70)return "Repeat the phrase in short sound groups and keep each Japanese vowel clear.";if(fluency<70)return "Use a steady rhythm. Pause between thought groups instead of stopping between every syllable.";if(pronunciation<85)return "Your meaning is understandable. Repeat once more while matching Sumi's pacing and vowel length.";return "Your pronunciation is clear and complete. Keep the same relaxed pace in the next exchange.";}
    private String practiceTip(double pronunciation,double accuracy,double fluency,double completeness){if(completeness<70)return "First read the whole phrase silently, then speak it from beginning to end in one attempt.";if(accuracy<70)return "Listen for long vowels, doubled consonants, and the difference between short and long sounds.";if(fluency<70)return "Tap a gentle beat for each mora and avoid rushing the polite ending.";return "Say the model once slowly, then repeat it at a natural conversational speed.";}
    private void convert(Path input,Path output)throws Exception{Process p=new ProcessBuilder("ffmpeg","-nostdin","-hide_banner","-loglevel","error","-y","-i",input.toString(),"-ac","1","-ar","16000","-c:a","pcm_s16le",output.toString()).start();if(!p.waitFor(30,TimeUnit.SECONDS)){p.destroyForcibly();throw new IOException("Audio conversion timed out.");}if(p.exitValue()!=0||Files.size(output)<=44)throw new IOException("The recording could not be processed.");}
}
