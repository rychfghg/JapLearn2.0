package japlearn.demo.Service;

public interface LiveConversationProvider {
    record LiveAccess(String token, String model, String websocketUrl, String voice, String systemInstruction) {}
    LiveAccess createGuidedPhraseAccess(String systemInstruction) throws Exception;
    default LiveAccess createTalkWithSumiAccess(String systemInstruction) throws Exception { return createGuidedPhraseAccess(systemInstruction); }
    boolean configured();
    String providerName();
}
