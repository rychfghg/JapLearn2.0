package japlearn.demo.Entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "student_progress")
public class StudentProgress {

    @Id
    private String email;  // Assuming the student's email is unique

    private boolean hiragana1 = false;
    private boolean hiragana2 = false;
    private boolean hiragana3 = false;
    private boolean katakana1 = false;
    private boolean katakana2 = false;
    private boolean katakana3 = false;
    private boolean vocab1 = false;
    private boolean vocab2 = false;
    private boolean sentence = false;
    private boolean badge1 = false;
    private boolean badge2 = false;
    private boolean badge3 = false;

    // Getters and Setters

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isHiragana1() {
        return hiragana1;
    }

    public void setHiragana1(boolean hiragana1) {
        this.hiragana1 = hiragana1;
    }

    public boolean isHiragana2() {
        return hiragana2;
    }

    public void setHiragana2(boolean hiragana2) {
        this.hiragana2 = hiragana2;
    }

    public boolean isHiragana3() {
        return hiragana3;
    }

    public void setHiragana3(boolean hiragana3) {
        this.hiragana3 = hiragana3;
    }

    public boolean isKatakana1() {
        return katakana1;
    }

    public void setKatakana1(boolean katakana1) {
        this.katakana1 = katakana1;
    }

    public boolean isKatakana2() {
        return katakana2;
    }

    public void setKatakana2(boolean katakana2) {
        this.katakana2 = katakana2;
    }

    public boolean isKatakana3() {
        return katakana3;
    }

    public void setKatakana3(boolean katakana3) {
        this.katakana3 = katakana3;
    }

    public boolean isVocab1() {
        return vocab1;
    }

    public void setVocab1(boolean vocab1) {
        this.vocab1 = vocab1;
    }

    public boolean isVocab2() {
        return vocab2;
    }

    public void setVocab2(boolean vocab2) {
        this.vocab2 = vocab2;
    }

    public boolean isSentence() {
        return sentence;
    }

    public void setSentence(boolean sentence) {
        this.sentence = sentence;
    }

    public boolean isBadge3() {
        return badge3;
    }

    public void setBadge3(boolean badge3) {
        this.badge3 = badge3;
    }

    public void setBadge2(boolean badge2) {
        this.badge2 = badge2;
    }

    public boolean isBadge1() {
        return badge1;
    }

    public void setBadge1(boolean badge1) {
        this.badge1 = badge1;
    }

    public boolean isBadge2() {
        return badge2;
    }

 
    
}
