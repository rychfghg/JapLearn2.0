package japlearn.demo.Service;

public interface LiveConversationProvider {
    record LiveAccess(String token, String model, String websocketUrl, String voice, int remainingSeconds) {}
    LiveAccess createGuidedPhraseAccess(String systemInstruction, int remainingSeconds) throws Exception;
    boolean configured();
    String providerName();
}
