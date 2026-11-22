package com.managify.projects.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.managify.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The project tracker end to end: real MySQL and Redis, mocked Keycloak. */
class ProjectTrackerIntegrationTest extends IntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    StringRedisTemplate redis;

    @Test
    void buildReorderConvertAssignAndDelete() throws Exception {
        long projectId = createProject();
        long workerId = createWorker();

        long a = id(call(post(tracker(projectId, "/tasks")), Map.of("title", "A"), 201), "A");
        call(post(tracker(projectId, "/tasks")), Map.of("title", "B"), 201);
        JsonNode tree = call(post(tracker(projectId, "/tasks")), Map.of("title", "C"), 201);
        long b = id(tree, "B");
        long c = id(tree, "C");
        assertThat(taskTitles(tree)).containsExactly("A", "B", "C");

        call(post(tracker(projectId, "/tasks/" + a + "/tasklets")),
                Map.of("title", "a1", "startDate", "2027-01-05", "endDate", "2027-01-10"), 201);
        tree = call(post(tracker(projectId, "/tasks/" + a + "/tasklets")),
                Map.of("title", "a2", "startDate", "2027-01-08", "endDate", "2027-01-20"), 201);
        long a1 = id(tree, "a1");
        long a2 = id(tree, "a2");
        JsonNode taskA = task(tree, "A");
        assertThat(taskA.get("datesDerived").asBoolean()).isTrue();
        assertThat(taskA.get("startDate").asText()).isEqualTo("2027-01-05");
        assertThat(taskA.get("endDate").asText()).isEqualTo("2027-01-20");
        assertThat(taskA.get("durationDays").asLong()).isEqualTo(16);

        // Move up / down
        tree = call(post(tracker(projectId, "/items/" + c + "/move-up")), null, 200);
        assertThat(taskTitles(tree)).containsExactly("A", "C", "B");
        tree = call(post(tracker(projectId, "/items/" + a + "/move-down")), null, 200);
        assertThat(taskTitles(tree)).containsExactly("C", "A", "B");

        // A task becomes a tasklet, and a tasklet becomes a task
        tree = call(post(tracker(projectId, "/items/" + b + "/make-tasklet")), Map.of("parentTaskId", a), 200);
        assertThat(taskTitles(tree)).containsExactly("C", "A");
        assertThat(taskletTitles(tree, "A")).containsExactly("a1", "a2", "B");

        tree = call(post(tracker(projectId, "/items/" + a1 + "/make-task")), null, 200);
        assertThat(taskTitles(tree)).containsExactly("C", "A", "a1");
        assertThat(taskletTitles(tree, "A")).containsExactly("a2", "B");
        assertThat(positions(task(tree, "A").get("tasklets"))).containsExactly(0, 1);

        // Only two levels: a task that has tasklets cannot become a tasklet
        call(post(tracker(projectId, "/items/" + a + "/make-tasklet")), Map.of("parentTaskId", c), 409);

        // Assignment
        tree = call(put(tracker(projectId, "/items/" + a2 + "/assignee")), Map.of("workerId", workerId), 200);
        JsonNode assignee = tasklet(tree, "A", "a2").get("assignee");
        assertThat(assignee.get("id").asLong()).isEqualTo(workerId);
        assertThat(assignee.get("imageUrl").asText()).isEqualTo("https://example.com/avatars/ada.png");
        JsonNode items = call(get("/api/v1/workers/" + workerId + "/items"), null, 200).get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("parentTaskTitle").asText()).isEqualTo("A");

        // Deleting a task deletes its tasklets; the worker's list is refreshed
        tree = call(delete(tracker(projectId, "/items/" + a)), null, 200);
        assertThat(taskTitles(tree)).containsExactly("C", "a1");
        assertThat(positions(tree.get("tasks"))).containsExactly(0, 1);
        assertThat(tree.get("taskletCount").asInt()).isZero();
        assertThat(call(get("/api/v1/workers/" + workerId + "/items"), null, 200).get("items")).isEmpty();
    }

    @Test
    void trackerIsCachedInRedisAndEvictedOnWrite() throws Exception {
        long projectId = createProject();
        String key = "managify::projectTracker::" + projectId;
        call(post(tracker(projectId, "/tasks")), Map.of("title", "First"), 201);

        call(get(tracker(projectId, "/tracker")), null, 200);
        assertThat(redis.hasKey(key)).isTrue();

        JsonNode cached = call(get(tracker(projectId, "/tracker")), null, 200);
        assertThat(taskTitles(cached)).containsExactly("First");

        call(post(tracker(projectId, "/tasks")), Map.of("title", "Second"), 201);
        assertThat(redis.hasKey(key)).isFalse();

        assertThat(taskTitles(call(get(tracker(projectId, "/tracker")), null, 200))).containsExactly("First", "Second");
    }

    @Test
    void errorsMapToTheRightStatus() throws Exception {
        mvc.perform(get("/api/v1/projects")).andExpect(status().isUnauthorized());

        call(post("/api/v1/projects"), Map.of("name", unique(), "startDate", "2027-02-01", "endDate", "2027-01-01"), 400);
        call(get("/api/v1/projects/999999/tracker"), null, 404);
        call(post("/api/v1/workers"), Map.of("fullName", "Bad Image", "email", UUID.randomUUID() + "@example.com",
                "imageUrl", "not-a-url"), 400);

        long projectId = createProject();
        long task = id(call(post(tracker(projectId, "/tasks")), Map.of("title", "T"), 201), "T");
        long tasklet = id(call(post(tracker(projectId, "/tasks/" + task + "/tasklets")), Map.of("title", "t"), 201), "t");
        // Tasklets cannot have tasklets, and an item is already what it is
        call(post(tracker(projectId, "/tasks/" + tasklet + "/tasklets")), Map.of("title", "x"), 409);
        call(post(tracker(projectId, "/items/" + task + "/make-task")), null, 409);
        call(patch(tracker(projectId, "/items/" + task)), Map.of("startDate", "2027-05-02", "endDate", "2027-05-01"), 400);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static RequestPostProcessor user() {
        return jwt().jwt(j -> j.claim("preferred_username", "tester"))
                .authorities(new SimpleGrantedAuthority("user"));
    }

    private static String tracker(long projectId, String path) {
        return "/api/v1/projects/" + projectId + path;
    }

    private static String unique() {
        return "Project " + UUID.randomUUID();
    }

    private long createProject() throws Exception {
        JsonNode project = call(post("/api/v1/projects"),
                Map.of("name", unique(), "startDate", "2027-01-01", "endDate", "2027-03-31"), 201);
        assertThat(project.get("durationDays").asLong()).isEqualTo(90);
        return project.get("id").asLong();
    }

    private long createWorker() throws Exception {
        return call(post("/api/v1/workers"),
                Map.of("fullName", "Ada Worker", "email", UUID.randomUUID() + "@example.com",
                        "imageUrl", "https://example.com/avatars/ada.png"), 201)
                .get("id").asLong();
    }

    /** Performs the call as a user, checks the status and returns the response's {@code data}. */
    private JsonNode call(MockHttpServletRequestBuilder request, Object body, int expectedStatus) throws Exception {
        request.with(user()).contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            request.content(json.writeValueAsString(body));
        }
        MvcResult result = mvc.perform(request).andExpect(status().is(expectedStatus)).andReturn();
        JsonNode root = json.readTree(result.getResponse().getContentAsString());
        return root.get("data");
    }

    private static List<String> taskTitles(JsonNode tree) {
        List<String> titles = new ArrayList<>();
        tree.get("tasks").forEach(t -> titles.add(t.get("title").asText()));
        return titles;
    }

    private static List<String> taskletTitles(JsonNode tree, String taskTitle) {
        List<String> titles = new ArrayList<>();
        task(tree, taskTitle).get("tasklets").forEach(t -> titles.add(t.get("title").asText()));
        return titles;
    }

    private static List<Integer> positions(JsonNode nodes) {
        List<Integer> positions = new ArrayList<>();
        nodes.forEach(n -> positions.add(n.get("position").asInt()));
        return positions;
    }

    private static JsonNode task(JsonNode tree, String title) {
        for (JsonNode t : tree.get("tasks")) {
            if (t.get("title").asText().equals(title)) {
                return t;
            }
        }
        throw new AssertionError("No task " + title);
    }

    private static JsonNode tasklet(JsonNode tree, String taskTitle, String title) {
        for (JsonNode t : task(tree, taskTitle).get("tasklets")) {
            if (t.get("title").asText().equals(title)) {
                return t;
            }
        }
        throw new AssertionError("No tasklet " + title);
    }

    /** The id of the task or tasklet with that title anywhere in the tree. */
    private static long id(JsonNode tree, String title) {
        for (JsonNode t : tree.get("tasks")) {
            if (t.get("title").asText().equals(title)) {
                return t.get("id").asLong();
            }
            for (JsonNode child : t.get("tasklets")) {
                if (child.get("title").asText().equals(title)) {
                    return child.get("id").asLong();
                }
            }
        }
        throw new AssertionError("No item " + title);
    }
}
