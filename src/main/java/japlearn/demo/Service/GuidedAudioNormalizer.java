package japlearn.demo.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GuidedAudioNormalizer {
 public byte[] toAzureWav(byte[] source,String contentType){
  if(source==null||source.length==0||source.length>12_000_000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"The recording is empty or too large.");
  if(contentType!=null&&contentType.toLowerCase().contains("wav"))return source;
  Path input=null,output=null;
  try{
   input=Files.createTempFile("guided-input-",".audio");output=Files.createTempFile("guided-output-",".wav");Files.write(input,source);
   Process process=new ProcessBuilder("ffmpeg","-nostdin","-loglevel","error","-y","-i",input.toString(),"-ac","1","-ar","16000","-c:a","pcm_s16le",output.toString()).redirectErrorStream(true).start();
   if(!process.waitFor(20,TimeUnit.SECONDS)||process.exitValue()!=0)throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"This recording format could not be prepared for speech evaluation.");
   byte[] wav=Files.readAllBytes(output);if(wav.length<44)throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"The recording did not contain usable speech.");return wav;
  }catch(ResponseStatusException e){throw e;}catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Audio conversion is unavailable on the server.",e);}finally{try{if(input!=null)Files.deleteIfExists(input);if(output!=null)Files.deleteIfExists(output);}catch(Exception ignored){}}
 }
 public byte[] wavToPcm(byte[] wav){
  if(wav==null||wav.length<=44)throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"The recording did not contain usable speech.");
  for(int i=12;i+8<=wav.length;){int size=(wav[i+4]&255)|((wav[i+5]&255)<<8)|((wav[i+6]&255)<<16)|((wav[i+7]&255)<<24);if(wav[i]=='d'&&wav[i+1]=='a'&&wav[i+2]=='t'&&wav[i+3]=='a'){int end=Math.min(wav.length,i+8+Math.max(0,size));return java.util.Arrays.copyOfRange(wav,i+8,end);}i+=8+Math.max(0,size)+(size&1);}
  throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"The normalized recording did not contain PCM audio.");
 }
}
