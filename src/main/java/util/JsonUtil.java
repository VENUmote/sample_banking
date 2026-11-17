package util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes the uniform JSON envelope used by every API endpoint:
 * <pre>{ "success": true|false, "message": "...", "data": {...}, "errors": {...} }</pre>
 */
public final class JsonUtil {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src, type, ctx) -> new JsonPrimitive(src.format(DATE_TIME)))
            .serializeNulls()
            .create();

    private JsonUtil() {
    }

    /** Serialises {@code body} as JSON with the given HTTP status. */
    public static void write(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write(GSON.toJson(body));
    }

    /** 200 OK success envelope. */
    public static void success(HttpServletResponse resp, String message, Object data) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", message);
        body.put("data", data);
        write(resp, HttpServletResponse.SC_OK, body);
    }

    /** Error envelope with an HTTP error status and optional field errors. */
    public static void error(HttpServletResponse resp, int status, String message,
                             Map<String, String> fieldErrors) throws IOException {
        error(resp, status, message, fieldErrors, null);
    }

    /** Error envelope that also carries data (e.g. the available balance). */
    public static void error(HttpServletResponse resp, int status, String message,
                             Map<String, String> fieldErrors, Object data) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message);
        body.put("errors", fieldErrors == null ? Map.of() : fieldErrors);
        body.put("data", data);
        write(resp, status, body);
    }
}
