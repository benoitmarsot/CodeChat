package com.unbumpkin.codechat.dto;

import java.io.File;

public record FileRenameDescriptor(File oldFile, File newFile, String mimeType) {
    
}
