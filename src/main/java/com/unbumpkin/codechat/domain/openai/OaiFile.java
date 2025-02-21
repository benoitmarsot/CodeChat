package com.unbumpkin.codechat.domain.openai;

public record OaiFile(
    int fId,
    int userId,
    int projectId,
    String fileId, 
    String fileName, 
    String rootdir, 
    String filePath, 
    Purposes purpose,
    int linecount
) {
    /**
     * Enum for the purposes of the file upload
     * - "assistants" for Assistants and Message files
     * - "vision" for Assistants image file inputs, 
     * - "batch" for Batch API, 
     * - "fine-tune" for Fine-tuning.
     */
    public enum Purposes {
        assistants("assistants"),
        vision("vision"),
        batch("batch"),
        fine_tune("fine-tune");
        private String purpose;
        Purposes(String purpose) {
            this.purpose = purpose;
        }
        @Override
        public String toString() {
            return purpose;
        }
    }
    public OaiFile getUidLcOaiFile(int userId, int lineCount) {
        return new OaiFile(fId,userId,projectId,fileId,fileName,rootdir,filePath,purpose,linecount);
    }
}