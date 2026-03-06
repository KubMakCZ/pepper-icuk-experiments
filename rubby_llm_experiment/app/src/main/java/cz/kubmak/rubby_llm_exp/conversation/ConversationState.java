package cz.kubmak.rubby_llm_exp.conversation;

public enum ConversationState {
    IDLE,        // Ceka na klicove slovo / uzivatelsky vstup
    LISTENING,   // SpeechRecognizer zachytava rec
    PROCESSING,  // Ceka na odpoved z LLM
    SPEAKING     // Robot mluvi odpoved
}
