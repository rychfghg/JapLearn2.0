package japlearn.demo.Service;

public interface LiveConversationProvider {
    record LiveAccess(String token, String model, String websocketUrl, String voice) {}
    LiveAccess createGuidedPhraseAccess(String systemInstruction) throws Exception;
    boolean configured();
    String providerName();
}
