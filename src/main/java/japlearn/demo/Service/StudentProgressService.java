package japlearn.demo.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import japlearn.demo.Entity.StudentProgress;
import japlearn.demo.Repository.StudentProgressRepository;

@Service
public class StudentProgressService {

    @Autowired
    private StudentProgressRepository studentProgressRepository;

    // Method to get student's progress
    public StudentProgress getProgress(String email) {
        return studentProgressRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student progress not found for email: " + email));
    }

    // Method to check the state of a specific field (e.g., vocab1, badge1)
    public boolean checkFieldState(String email, String field) {
        StudentProgress progress = getProgress(email);

        // Check the state of the specified field
        switch (field) {
            case "vocab1" -> {
                return progress.isVocab1();
            }
            case "hiragana1" -> {
                return progress.isHiragana1();
            }
            case "hiragana2" -> {
                return progress.isHiragana2();
            }
            case "hiragana3" -> {
                return progress.isHiragana3();
            }
            case "katakana1" -> {
                return progress.isKatakana1();
            }
            case "katakana2" -> {
                return progress.isKatakana2();
            }
            case "katakana3" -> {
                return progress.isKatakana3();
            }
            case "vocab2" -> {
                return progress.isVocab2();
            }
            case "sentence" -> {
                return progress.isSentence();
            }
            case "badge1" -> {
                return progress.isBadge1();  // Check badge1
            }
            case "badge2" -> {
                return progress.isBadge2();  // Check badge2
            }
            case "badge3" -> {
                return progress.isBadge3();  // Check badge3
            }
            default -> throw new IllegalArgumentException("Invalid field: " + field);
        }
    }

    // Method to save a new record with default progress
    public StudentProgress saveProgress(String email) {
        // Create a new StudentProgress object
        StudentProgress newProgress = new StudentProgress();
        
        // Set the email and default progress fields
        newProgress.setEmail(email);
        newProgress.setHiragana1(true); // Assuming initial progress starts with hiragana1
        newProgress.setHiragana2(false);
        newProgress.setHiragana3(false);
        newProgress.setKatakana1(false);
        newProgress.setKatakana2(false);
        newProgress.setKatakana3(false);
        newProgress.setVocab1(false);
        newProgress.setVocab2(false);
        newProgress.setSentence(false);
        newProgress.setBadge1(false);  // Initially, badge1 is false
        newProgress.setBadge2(false);  // Initially, badge2 is false
        newProgress.setBadge3(false);  // Initially, badge3 is false

        // Save the new progress in the database
        return studentProgressRepository.save(newProgress);
    }

    // Method to update an individual field (e.g., katakana1, badge1)
    public StudentProgress updateSingleField(String email, String field, boolean value) {
        // Fetch the existing progress by email
        StudentProgress existingProgress = studentProgressRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student progress not found for email: " + email));

        // Update the field based on the provided field name
        switch (field) {
            case "hiragana1" -> existingProgress.setHiragana1(value);
            case "hiragana2" -> existingProgress.setHiragana2(value);
            case "hiragana3" -> existingProgress.setHiragana3(value);
            case "katakana1" -> existingProgress.setKatakana1(value);
            case "katakana2" -> existingProgress.setKatakana2(value);
            case "katakana3" -> existingProgress.setKatakana3(value);
            case "vocab1" -> existingProgress.setVocab1(value);
            case "vocab2" -> existingProgress.setVocab2(value);
            case "sentence" -> existingProgress.setSentence(value);
            case "badge1" -> existingProgress.setBadge1(value);  // Update badge1
            case "badge2" -> existingProgress.setBadge2(value);  // Update badge2
            case "badge3" -> existingProgress.setBadge3(value);  // Update badge3
            default -> throw new IllegalArgumentException("Invalid field: " + field);
        }

        // Save the updated progress
        return studentProgressRepository.save(existingProgress);
    }

    // Method to reset all progress fields for a student
    public StudentProgress resetProgress(String email) {
        StudentProgress existingProgress = studentProgressRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student progress not found for email: " + email));

        // Reset all fields to false
        existingProgress.setHiragana1(false);
        existingProgress.setHiragana2(false);
        existingProgress.setHiragana3(false);
        existingProgress.setKatakana1(false);
        existingProgress.setKatakana2(false);
        existingProgress.setKatakana3(false);
        existingProgress.setVocab1(false);
        existingProgress.setVocab2(false);
        existingProgress.setSentence(false);
        existingProgress.setBadge1(false);  // Reset badge1
        existingProgress.setBadge2(false);  // Reset badge2
        existingProgress.setBadge3(false);  // Reset badge3

        // Save the reset progress
        return studentProgressRepository.save(existingProgress);
    }
}
