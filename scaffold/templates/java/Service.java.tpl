package dev.fncm.service;

import dev.fncm.auth.TokenContext;
import dev.fncm.model.__NAME__Result;
import dev.fncm.service.javaapi.FileNetService;
import dev.fncm.service.javaapi.service.__NAME__Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.logging.Logger;

/**
 * Business logic for __NAME__.
 *
 * Inject this service into __NAME__Resource and call {@link #run(TokenContext)}.
 * Add methods here for more complex orchestration that should not live in the resource.
 */
@ApplicationScoped
public class __NAME__Service {

    private static final Logger LOGGER = Logger.getLogger(__NAME__Service.class.getName());

    @Inject
    FileNetService fileNetService;

    public __NAME__Result run(TokenContext tokenContext) throws Exception {
        return fileNetService.run(new __NAME__Operation(), tokenContext);
    }
}

// Made with Bob
