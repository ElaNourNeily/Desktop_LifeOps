package service;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import model.task.Tache;
import okhttp3.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Toggl Track integration (API v9).
 *
 * Security: reads credentials from environment variables / JVM properties:
 * - TOGGL_API_TOKEN  (or -Dtoggl.apiToken=...)
 * - TOGGL_WORKSPACE_ID (or -Dtoggl.workspaceId=...)
 */
public class TimeTrackingService {

    private static final String API_BASE = "https://api.track.toggl.com/api/v9";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient http;
    private final Gson gson = new Gson();
    private final Long workspaceId;
    private final String authHeader;
    private final boolean enabled;

    private final TaskService taskService;

    public TimeTrackingService() {
        this(new TaskService());
    }

    public TimeTrackingService(TaskService taskService) {
        this.taskService = Objects.requireNonNull(taskService);
        Long wid = null;
        String auth = null;
        boolean ok = false;
        try {
            wid = readWorkspaceId();
            auth = buildAuthHeader(readApiToken());
            ok = true;
        } catch (Exception ignored) {
            // Missing/invalid config -> service stays disabled and will fallback to local tracking.
        }
        this.workspaceId = wid;
        this.authHeader = auth;
        this.enabled = ok;
        this.http = new OkHttpClient.Builder()
                .callTimeout(25, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Starts a running Toggl timer and syncs:
     * - togglEntryId
     * - startTime
     * - endTime (null)
     * - realTimeSpentSeconds (unchanged)
     *
     * Duplicate protection: if the task already has a running entry, no-op.
     *
     * Fallback: if API fails, starts local tracking (stores startTime only).
     */
    public void startTimer(Tache task) {
        if (task == null) return;
        if (isRunning(task)) return; // Prevent duplicate timers

        Date start = Date.from(Instant.now());

        if (!enabled) {
            // Local-only fallback if Toggl isn't configured
            task.setTogglEntryId(null);
            task.setStartTime(start);
            task.setEndTime(null);
            taskService.update(task);
            return;
        }

        try {
            TogglCreateTimeEntryRequest payload = new TogglCreateTimeEntryRequest();
            payload.createdWith = "Task Desktop App";
            payload.description = buildDescription(task);
            payload.workspaceId = workspaceId;
            payload.duration = -1; // running
            payload.start = Instant.ofEpochMilli(start.getTime()).toString();
            payload.stop = null;

            Request req = new Request.Builder()
                    .url(API_BASE + "/workspaces/" + workspaceId + "/time_entries")
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(gson.toJson(payload), JSON))
                    .build();

            try (Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) {
                    throw new IOException("Toggl start failed HTTP " + resp.code());
                }
                String body = resp.body() != null ? resp.body().string() : "";
                TogglTimeEntry created = gson.fromJson(body, TogglTimeEntry.class);
                if (created == null || created.id == null) {
                    throw new IOException("Toggl start failed: missing entry id");
                }

                task.setTogglEntryId(created.id);
                task.setStartTime(parseDateOrFallback(created.start, start));
                task.setEndTime(null);
                taskService.update(task);
            }
        } catch (Exception apiError) {
            // Fallback to local tracking: store only startTime.
            task.setTogglEntryId(null);
            task.setStartTime(start);
            task.setEndTime(null);
            taskService.update(task);
        }
    }

    /**
     * Stops Toggl timer and syncs:
     * - endTime
     * - realTimeSpentSeconds
     *
     * Fallback: if API fails, computes local duration from startTime.
     */
    public void stopTimer(Tache task) {
        if (task == null) return;
        if (task.getEndTime() != null) return; // already stopped

        // If we never started (no Toggl entry), do a local stop if possible.
        if (task.getTogglEntryId() == null) {
            stopLocally(task);
            return;
        }

        if (!enabled) {
            stopLocally(task);
            return;
        }

        try {
            Request req = new Request.Builder()
                    .url(API_BASE + "/workspaces/" + workspaceId + "/time_entries/" + task.getTogglEntryId() + "/stop")
                    .header("Authorization", authHeader)
                    .patch(RequestBody.create(new byte[0], null))
                    .build();

            try (Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) {
                    throw new IOException("Toggl stop failed HTTP " + resp.code());
                }
                String body = resp.body() != null ? resp.body().string() : "";
                TogglTimeEntry stopped = gson.fromJson(body, TogglTimeEntry.class);
                if (stopped == null) {
                    throw new IOException("Toggl stop failed: empty response");
                }

                Date end = parseDateOrFallback(stopped.stop, Date.from(Instant.now()));
                task.setEndTime(end);

                Integer seconds = null;
                if (stopped.duration != null) {
                    // For stopped entries duration is seconds (positive).
                    // For running entries duration is negative epoch seconds; stop endpoint should return stopped.
                    seconds = stopped.duration >= 0 ? stopped.duration : null;
                }
                if (seconds == null) {
                    seconds = computeLocalSeconds(task.getStartTime(), end);
                }
                task.setRealTimeSpentSeconds(seconds);

                taskService.update(task);
            }
        } catch (Exception apiError) {
            stopLocally(task);
        }
    }

    /**
     * Fetch the Toggl entry for a taskId (if linked).
     */
    public TogglTimeEntry getTimeEntry(int taskId) {
        Tache task = taskService.getById(taskId);
        if (task == null || task.getTogglEntryId() == null) return null;
        return getTimeEntryByEntryId(task.getTogglEntryId());
    }

    public TogglTimeEntry getTimeEntryByEntryId(long entryId) {
        if (!enabled) return null;
        try {
            Request req = new Request.Builder()
                    .url(API_BASE + "/workspaces/" + workspaceId + "/time_entries/" + entryId)
                    .header("Authorization", authHeader)
                    .get()
                    .build();
            try (Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) return null;
                String body = resp.body() != null ? resp.body().string() : "";
                return gson.fromJson(body, TogglTimeEntry.class);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isRunning(Tache task) {
        return task.getTogglEntryId() != null && task.getStartTime() != null && task.getEndTime() == null;
    }

    private void stopLocally(Tache task) {
        Date end = Date.from(Instant.now());
        task.setEndTime(end);
        Integer seconds = computeLocalSeconds(task.getStartTime(), end);
        task.setRealTimeSpentSeconds(seconds);
        // keep togglEntryId as-is (if any) so you can reconcile later; but if it was null, it stays null
        taskService.update(task);
    }

    private Integer computeLocalSeconds(Date start, Date end) {
        if (start == null || end == null) return null;
        long ms = Math.max(0, end.getTime() - start.getTime());
        long sec = ms / 1000L;
        return sec > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sec;
    }

    private String buildDescription(Tache task) {
        String title = task.getTitre() != null ? task.getTitre().trim() : "Untitled";
        return "Task#" + task.getId() + " - " + title;
    }

    private Date parseDateOrFallback(String iso, Date fallback) {
        try {
            if (iso == null || iso.isBlank()) return fallback;
            return Date.from(Instant.parse(iso));
        } catch (Exception e) {
            return fallback;
        }
    }

    private String buildAuthHeader(String apiToken) {
        // Toggl Track expects HTTP Basic auth: "<token>:api_token"
        String raw = apiToken + ":api_token";
        String b64 = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        return "Basic " + b64;
    }

    private String readApiToken() {
        String env = System.getenv("TOGGL_API_TOKEN");
        if (env != null && !env.isBlank()) return env.trim();
        String prop = System.getProperty("toggl.apiToken");
        if (prop != null && !prop.isBlank()) return prop.trim();
        throw new IllegalStateException("Missing TOGGL_API_TOKEN (or -Dtoggl.apiToken=...)");
    }

    private long readWorkspaceId() {
        String env = System.getenv("TOGGL_WORKSPACE_ID");
        String val = (env != null && !env.isBlank()) ? env.trim() : System.getProperty("toggl.workspaceId");
        if (val == null || val.isBlank()) {
            throw new IllegalStateException("Missing TOGGL_WORKSPACE_ID (or -Dtoggl.workspaceId=...)");
        }
        return Long.parseLong(val);
    }

    // --- Minimal DTOs for Toggl API v9 ---

    public static class TogglCreateTimeEntryRequest {
        @SerializedName("created_with")
        public String createdWith;
        public String description;
        public Long workspace_id;
        public Long wid;
        @SerializedName("workspace_id")
        public Long workspaceId;
        public Integer duration;
        public String start;
        public String stop;
    }

    public static class TogglTimeEntry {
        public Long id;
        @SerializedName("workspace_id")
        public Long workspaceId;
        public String description;
        public String start;
        public String stop;
        public Integer duration;
        public String at;
    }
}

