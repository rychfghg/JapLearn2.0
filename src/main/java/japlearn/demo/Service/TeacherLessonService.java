package japlearn.demo.Service;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import japlearn.demo.Entity.*;
import japlearn.demo.Repository.*;

@Service
public class TeacherLessonService {
 private final LessonRepository lessons; private final LessonQuizAttemptRepository attempts; private final GridFsTemplate files;
 private final ClassesRepository classes; private final StudentRepository students;
 public TeacherLessonService(LessonRepository l,LessonQuizAttemptRepository a,ClassesRepository c,StudentRepository s,GridFsTemplate f){lessons=l;attempts=a;classes=c;students=s;files=f;}
 private void ownClass(String email,String code){classes.findByClassCodesAndOwnerTeacherEmailIgnoreCase(code,email).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"This class belongs to another teacher"));}
 public Lesson create(String teacher,Lesson lesson,MultipartFile pdf){
  validate(lesson); List<String> targets=lesson.getClassIds().stream().filter(code->code!=null&&!code.isBlank()).map(String::trim).distinct().toList();
  if(targets.isEmpty())targets=List.of(lesson.getClassId().trim()); for(String code:targets)ownClass(teacher,code);
  lesson.setClassIds(targets); lesson.setClassId(targets.get(0));
  lesson.setId(null); lesson.setOwnerTeacherEmail(teacher); lesson.setCreatedAt(Instant.now()); lesson.setPublished(true);
  if(pdf==null||pdf.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A PDF lesson file is required");String filename=pdf.getOriginalFilename()==null?"lesson.pdf":pdf.getOriginalFilename();if(!"application/pdf".equalsIgnoreCase(pdf.getContentType())&&!filename.toLowerCase(Locale.ROOT).endsWith(".pdf"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Upload a PDF file");
  if(pdf.getSize()>10_000_000)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"PDF must be 10 MB or smaller"); lesson.setSourceFileName(filename); lesson.setPdfPageCount(pageCount(pdf));
  try{ObjectId fileId=files.store(pdf.getInputStream(),filename,"application/pdf");lesson.setPdfFileId(fileId.toHexString());return lessons.save(lesson);}catch(IOException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"The PDF could not be stored");}
 }
 private int pageCount(MultipartFile file){try(PDDocument doc=Loader.loadPDF(file.getBytes())){if(doc.getNumberOfPages()<1)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"The PDF has no pages");return doc.getNumberOfPages();}catch(IOException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"The PDF could not be read");}}
 private void validate(Lesson l){if(l==null||blank(l.getClassId())||blank(l.getLesson_title())||blank(l.getLesson_type()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Class, title and lesson type are required"); if(l.getQuizTimerSeconds()!=null&&(l.getQuizTimerSeconds()<15||l.getQuizTimerSeconds()>7200))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Quiz timer must be between 15 seconds and 2 hours"); for(Lesson.QuizQuestion q:l.getQuiz()){q.setType(q.getType()==null?"MULTIPLE_CHOICE":q.getType().toUpperCase()); if(blank(q.getPrompt())||blank(q.getCorrectAnswer()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Every quiz question needs a prompt and correct answer"); if("TRUE_FALSE".equals(q.getType()))q.setChoices(List.of("True","False")); if("MULTIPLE_CHOICE".equals(q.getType())&&(q.getChoices().size()<2||!q.getChoices().contains(q.getCorrectAnswer())))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Multiple-choice answers must match one of the choices");}}
 public List<Lesson> teacherList(String teacher){return lessons.findByOwnerTeacherEmailIgnoreCaseOrderByCreatedAtDesc(teacher);}
 public Lesson owned(String teacher,String id){Lesson l=lessons.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Lesson not found")); if(!teacher.equalsIgnoreCase(l.getOwnerTeacherEmail()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This lesson belongs to another teacher");return l;}
 public void delete(String teacher,String id){Lesson lesson=owned(teacher,id);attempts.deleteAll(attempts.findByLessonIdOrderBySubmittedAtDesc(id));if(lesson.getPdfFileId()!=null&&ObjectId.isValid(lesson.getPdfFileId()))files.delete(Query.query(Criteria.where("_id").is(new ObjectId(lesson.getPdfFileId()))));lessons.deleteById(id);}
 public List<LessonQuizAttempt> results(String teacher,String id){owned(teacher,id);return attempts.findByLessonIdOrderBySubmittedAtDesc(id);}
 public List<Lesson> studentList(String email){Student s=students.findByEmail(email);if(s==null||blank(s.getClassCode()))return List.of();List<Lesson> out=new ArrayList<>(lessons.findByClassIdsContainingAndPublishedTrueOrderByCreatedAtDesc(s.getClassCode()));for(Lesson l:lessons.findByClassIdAndPublishedTrueOrderByCreatedAtDesc(s.getClassCode()))if(out.stream().noneMatch(x->x.getId().equals(l.getId())))out.add(l);out.sort((a,b)->{Instant x=a.getCreatedAt()==null?Instant.EPOCH:a.getCreatedAt(),y=b.getCreatedAt()==null?Instant.EPOCH:b.getCreatedAt();return y.compareTo(x);});return out;}
 public Lesson studentLesson(String email,String id){Student s=students.findByEmail(email);Lesson l=lessons.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Lesson not found"));boolean assigned=s!=null&&(l.getClassIds().stream().anyMatch(code->code.equalsIgnoreCase(s.getClassCode()))||(l.getClassIds().isEmpty()&&l.getClassId()!=null&&l.getClassId().equalsIgnoreCase(s.getClassCode())));if(!l.isPublished()||!assigned)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Lesson is not assigned to this student");return l;}
 public GridFsResource studentPdf(String email,String id){Lesson lesson=studentLesson(email,id);if(lesson.getPdfFileId()==null||!ObjectId.isValid(lesson.getPdfFileId()))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"This lesson does not have a PDF file");GridFSFile file=files.findOne(Query.query(Criteria.where("_id").is(new ObjectId(lesson.getPdfFileId()))));if(file==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"The lesson PDF is unavailable");return files.getResource(file);}
 public byte[] studentPdfPage(String email,String id,int page){GridFsResource resource=studentPdf(email,id);try(PDDocument document=Loader.loadPDF(resource.getInputStream().readAllBytes());ByteArrayOutputStream output=new ByteArrayOutputStream()){if(page<1||page>document.getNumberOfPages())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"PDF page not found");ImageIO.write(new PDFRenderer(document).renderImageWithDPI(page-1,135),"png",output);return output.toByteArray();}catch(IOException e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"The lesson slide could not be rendered");}}
 public LessonQuizAttempt submit(String email,String id,List<String> answers){Lesson l=studentLesson(email,id);Student s=students.findByEmail(email);List<String> safe=answers==null?List.of():answers;int score=0;for(int i=0;i<l.getQuiz().size();i++){String answer=i<safe.size()?safe.get(i):"";if(normal(answer).equals(normal(l.getQuiz().get(i).getCorrectAnswer())))score++;}LessonQuizAttempt a=new LessonQuizAttempt();a.setLessonId(id);a.setClassCode(s.getClassCode());a.setStudentEmail(email);a.setStudentName((s.getFname()+" "+s.getLname()).trim());a.setScore(score);a.setMaxScore(l.getQuiz().size());a.setPercentage(l.getQuiz().isEmpty()?0:Math.round(score*10000.0/l.getQuiz().size())/100.0);a.setAnswers(safe);a.setSubmittedAt(Instant.now());return attempts.save(a);}
 private static boolean blank(String s){return s==null||s.isBlank();} private static String normal(String s){return s==null?"":s.trim().toLowerCase(Locale.ROOT).replaceAll("[。.!?\\s]+$","");}
}
