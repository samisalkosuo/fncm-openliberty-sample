package dev.fncm.service.javaapi;

import com.filenet.api.core.ObjectStore;

/**
 * Plug-in contract for a single FileNet JACE operation.
 * Implement this interface and pass an instance to {@link FileNetService#run} —
 * auth, SSL, connect/disconnect lifecycle are all handled by the service.
 *
 * @param <T> the result type returned by the operation
 */
public interface FileNetOperation<T> {

    T execute(ObjectStore os, String username) throws Exception;
}

// Made with Bob
