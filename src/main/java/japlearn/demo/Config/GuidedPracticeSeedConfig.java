package japlearn.demo.Config;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import japlearn.demo.Entity.GuidedPracticeScenario;
import japlearn.demo.Repository.GuidedPracticeScenarioRepository;

@Configuration
public class GuidedPracticeSeedConfig {
 @Bean CommandLineRunner seedGuidedPractice(GuidedPracticeScenarioRepository repo){ return args -> {
  if(repo.count()>0) return;
  GuidedPracticeScenario s=new GuidedPracticeScenario(); s.setTitle("First Day at Work"); s.setCategory("EMPLOYMENT"); s.setRoleName("Tanaka-san, your coworker");
  s.setIntroduction("You have just arrived at a workplace in Japan. Greet your new coworker and introduce yourself politely.");
  s.setObjective("Exchange greetings, introduce yourself, and answer a simple personal question politely.");
  s.setAllowedTopics(List.of("workplace greetings","self-introduction","country of origin","asking for clarification"));
  s.setAllowedVocabulary(List.of("はじめまして","です","よろしくお願いします","から来ました","仕事")); s.setAllowedGrammar(List.of("Nです","Nから来ました","よろしくお願いします"));
  s.setTargetExpressions(List.of("はじめまして","レイです","よろしくお願いします","フィリピンから来ました","もう一度お願いします"));
  s.setProgressiveHints(List.of("Listen for Sumi's main question.","Useful vocabulary: 名前, から, お願いします.","Try a pattern such as 「___です」 or 「___から来ました」.","Example: 「レイです。よろしくお願いします。」"));
  s.setNodes(List.of()); repo.save(s);
 };}
}
