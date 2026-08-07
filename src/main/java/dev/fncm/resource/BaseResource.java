package dev.fncm.resource;

import dev.fncm.auth.TokenCache;
import dev.fncm.auth.TokenContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;

/**
 * Shared base for all JAX-RS resource classes.
 *
 * <p>Provides:
 * <ul>
 *   <li>Common {@link TokenContext} and {@link TokenCache} injection points.</li>
 *   <li>{@link #execute(ThrowingSupplier)} — runs an action and wraps any exception in a uniform
 *       JSON error response so every subclass error path looks the same.</li>
 *   <li>{@link #error(int, String)} — builds a {@code {"error":"…"}} response for the
 *       rare cases where a subclass needs to return a non-200 response directly.</li>
 * </ul>
 *
 * <p>Adding a new resource:
 * <pre>{@code
 * @Path("/mypath")
 * public class MyResource extends BaseResource {
 *     @Inject MyService myService;
 *
 *     @GET
 *     public Response get() {
 *         return execute(() -> myService.doWork(tokenContext));
 *     }
 * }
 * }</pre>
 */
public abstract class BaseResource {

    /** Supplier that may throw a checked exception — used by {@link #execute(ThrowingSupplier)}. */
    @FunctionalInterface
    protected interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    @Inject
    protected TokenContext tokenContext;

    @Inject
    protected TokenCache tokenCache;

    /**
     * Executes {@code action} and wraps the result in a 200 OK response.
     * <ul>
     *   <li>{@link IllegalStateException} → 503 Service Unavailable</li>
     *   <li>Any other {@link Exception}   → 500 Internal Server Error</li>
     * </ul>
     *
     * @param action supplier that produces the response body (must not return {@code null})
     * @param <T>    return type of the action
     * @return JAX-RS {@link Response}
     */
    protected <T> Response execute(ThrowingSupplier<T> action) {
        try {
            return Response.ok(action.get()).build();
        } catch (IllegalStateException e) {
            return error(503, e.getMessage());
        } catch (Exception e) {
            return error(500, e.getMessage());
        }
    }

    /**
     * Variant of {@link #execute(ThrowingSupplier)} for actions that already produce a
     * fully-built {@link Response} (e.g. streaming downloads). The response is returned
     * as-is rather than being wrapped in a new {@code 200 OK}.
     *
     * <ul>
     *   <li>{@link IllegalStateException} → 503 Service Unavailable</li>
     *   <li>Any other {@link Exception}   → 500 Internal Server Error</li>
     * </ul>
     *
     * @param action supplier that produces a ready-to-send {@link Response}
     * @return the {@link Response} from the action, or an error response
     */
    protected Response executeResponse(ThrowingSupplier<Response> action) {
        try {
            return action.get();
        } catch (IllegalStateException e) {
            return error(503, e.getMessage());
        } catch (Exception e) {
            return error(500, e.getMessage());
        }
    }

    /**
     * Builds a JSON error response: {@code {"error":"<message>"}}.
     *
     * @param status  HTTP status code
     * @param message error detail (may be {@code null})
     * @return JAX-RS {@link Response} with the given status and JSON body
     */
    protected Response error(int status, String message) {
        return Response.status(status)
                .entity(new JSONObject().put("error", message).toString())
                .build();
    }
}

// Made with Bob
