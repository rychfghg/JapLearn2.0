package japlearn.demo.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import japlearn.demo.Entity.*;
import japlearn.demo.Repository.*;

@Service
public class TeacherLessonService {
 private final LessonRepository lessons; private final LessonQuizAttemptRepository attempts;
 private final ClassesRepository classes; private final StudentRepository students;
 public TeacherLessonService(LessonRepository l,LessonQuizAttemptRepository a,ClassesRepository c,StudentRepository s){lessons=l;attempts=a;classes=c;students=s;}
 private void ownClass(String email,String code){classes.findByClassCodesAndOwnerTeacherEmailIgnoreCase(code,email).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"This class belongs to another teacher"));}
 public Lesson create(String teacher,Lesson lesson,MultipartFile pdf){
  ownClass(teacher,lesson.getClassId()); validate(lesson);
  lesson.setId(null); lesson.setOwnerTeacherEmail(teacher); lesson.setCreatedAt(Instant.now()); lesson.setPublished(true);
  if(pdf!=null&&!pdf.isEmpty()){String filename=pdf.getOriginalFilename()==null?"lesson.pdf":pdf.getOriginalFilename();if(!"application/pdf".equalsIgnoreCase(pdf.getContentType())&&!filename.toLowerCase(Locale.ROOT).endsWith(".pdf"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Upload a PDF file");
   if(pdf.getSize()>10_000_000)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"PDF must be 10 MB or smaller"); lesson.setSourceFileName(filename); lesson.setSections(extract(pdf));}
  if(lesson.getSections().isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Add lesson content or upload a readable PDF"); return lessons.save(lesson);
 }
 private List<Lesson.LessonSection> extract(MultipartFile file){try(PDDocument doc=Loader.loadPDF(file.getBytes())){String text=new PDFTextStripper().getText(doc).replace("\u0000","").trim(); if(text.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"This PDF has no selectable text. Use a text-based PDF or add content manually."); String[] paragraphs=text.split("(?:\\r?\\n){2,}"); List<Lesson.LessonSection> out=new ArrayList<>(); for(String p:paragraphs){String clean=p.replaceAll("[ \\t]+"," ").trim(); if(clean.isBlank())continue; String[] lines=clean.split("\\R",2); out.add(new Lesson.LessonSection(lines[0].length()<=90?lines[0]:"Lesson note",lines.length>1?lines[1]:clean)); if(out.size()==30)break;} return out;}catch(IOException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"The PDF could not be read");}}
 private void validate(Lesson l){if(l==null||blank(l.getClassId())||blank(l.getLesson_title())||blank(l.getLesson_type()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Class, title and lesson type are required"); if(l.getQuizTimerSeconds()!=null&&(l.getQuizTimerSeconds()<15||l.getQuizTimerSeconds()>7200))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Quiz timer must be between 15 seconds and 2 hours"); for(Lesson.QuizQuestion q:l.getQuiz()){q.setType(q.getType()==null?"MULTIPLE_CHOICE":q.getType().toUpperCase()); if(blank(q.getPrompt())||blank(q.getCorrectAnswer()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Every quiz question needs a prompt and correct answer"); if("TRUE_FALSE".equals(q.getType()))q.setChoices(List.of("True","False")); if("MULTIPLE_CHOICE".equals(q.getType())&&(q.getChoices().size()<2||!q.getChoices().contains(q.getCorrectAnswer())))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Multiple-choice answers must match one of the choices");}}
 public List<Lesson> teacherList(String teacher){return lessons.findByOwnerTeacherEmailIgnoreCaseOrderByCreatedAtDesc(teacher);}
 public Lesson owned(String teacher,String id){Lesson l=lessons.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Lesson not found")); if(!teacher.equalsIgnoreCase(l.getOwnerTeacherEmail()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This lesson belongs to another teacher");return l;}
 public void delete(String teacher,String id){owned(teacher,id);attempts.deleteAll(attempts.findByLessonIdOrderBySubmittedAtDesc(id));lessons.deleteById(id);}
 public List<LessonQuizAttempt> results(String teacher,String id){owned(teacher,id);return attempts.findByLessonIdOrderBySubmittedAtDesc(id);}
 public List<Lesson> studentList(String email){Student s=students.findByEmail(email);if(s==null||blank(s.getClassCode()))return List.of();return lessons.findByClassIdAndPublishedTrueOrderByCreatedAtDesc(s.getClassCode());}
 public Lesson studentLesson(String email,String id){Student s=students.findByEmail(email);Lesson l=lessons.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Lesson not found"));if(s==null||!l.isPublished()||!l.getClassId().equalsIgnoreCase(s.getClassCode()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Lesson is not assigned to this student");return l;}
 public LessonQuizAttempt submit(String email,String id,List<String> answers){Lesson l=studentLesson(email,id);Student s=students.findByEmail(email);List<String> safe=answers==null?List.of():answers;int score=0;for(int i=0;i<l.getQuiz().size();i++){String answer=i<safe.size()?safe.get(i):"";if(normal(answer).equals(normal(l.getQuiz().get(i).getCorrectAnswer())))score++;}LessonQuizAttempt a=new LessonQuizAttempt();a.setLessonId(id);a.setClassCode(l.getClassId());a.setStudentEmail(email);a.setStudentName((s.getFname()+" "+s.getLname()).trim());a.setScore(score);a.setMaxScore(l.getQuiz().size());a.setPercentage(l.getQuiz().isEmpty()?0:Math.round(score*10000.0/l.getQuiz().size())/100.0);a.setAnswers(safe);a.setSubmittedAt(Instant.now());return attempts.save(a);}
 private static boolean blank(String s){return s==null||s.isBlank();} private static String normal(String s){return s==null?"":s.trim().toLowerCase(Locale.ROOT).replaceAll("[。.!?\\s]+$","");}
}
