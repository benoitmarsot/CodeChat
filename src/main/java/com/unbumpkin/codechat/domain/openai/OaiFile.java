package com.unbumpkin.codechat.domain.openai;

public record OaiFile(
    String fileId, 
    String fileName, 
    String rootdir, 
    String filePath, 
    Purposes purpose
) {
    /**
     * Enum for the purposes of the file upload
     * - "assistants" for Assistants and Message files
     * - "vision" for Assistants image file inputs, 
     * - "batch" for Batch API, 
     * - "fine-tune" for Fine-tuning.
     */
    public enum Purposes {
        assistants,
        vision,
        batch,
        fine_tune
    }

}