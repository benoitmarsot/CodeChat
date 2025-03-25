package com.unbumpkin.codechat.dto;

import java.io.File;

public record FileRenameDescriptor(String oldFileName, String oldFilePath, File newFile, String mimeType) {
    
}
