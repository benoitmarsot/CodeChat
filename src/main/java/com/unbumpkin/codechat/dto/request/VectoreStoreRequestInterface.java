package com.unbumpkin.codechat.dto.request;

import java.util.Map;

/**
 * Interface for Vector Store Request
 *
 * Used to define the structure of requests made to the vector store.
 * This interface includes methods to retrieve the file ID and attributes
 * associated with the request.
 *  
 * @author unbumpkin
 * @version 1.0
 */
public interface VectoreStoreRequestInterface {
    public String file_id();
    public Map<String,String> attributes();
}
