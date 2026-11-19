package com.dailyai.assistant.controller;

import com.dailyai.assistant.dto.RefreshResultDto;
import com.dailyai.assistant.entity.Article;
import com.dailyai.assistant.entity.DailyTask;
import com.dailyai.assistant.entity.LearningTopic;
import com.dailyai.assistant.repository.ArticleRepository;
import com.dailyai.assistant.repository.DailyTaskRepository;
import com.dailyai.assistant.repository.LearningTopicRepository;
import com.dailyai.assistant.service.AdminService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiIntegrationTest {

    private static final String PASSWORD = "StrongPass1!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ArticleRepository articleRepository;
    @Autowired
    private LearningTopicRepository topicRepository;
    @Autowired
    private DailyTaskRepository dailyTaskRepository;

    @MockBean
    private AdminService adminService;

    private Article article;
    private LearningTopic topic;
    private DailyTask task;

    @BeforeEach
    void seedContent() {
        String suffix = String.valueOf(System.nanoTime());
        article = new Article();
        article.setTitle("Shared public AI update");
        article.setUrl("https://example.com/phase7-" + suffix);
        article.setDescription("A public article used to verify ownership of bookmarks and reading history.");
        article.setPublishedAt(Instant.now());
        article = articleRepository.save(article);

        topic = new LearningTopic();
        topic.setLevel(1);
        topic.setLevelName("Fundamentals");
        topic.setTitle("Private topic progress");
        topic.setSlug("phase7-topic-" + suffix);
        topic.setOrderIndex(1);
        topic = topicRepository.save(topic);

        task = new DailyTask();
        task.setDayNumber(1);
        task.setTitle("Private daily task");
        task.setKind(DailyTask.Kind.LEARN);
        task.setEstimatedMinutes(10);
        task.setOrderIndex(1);
        task = dailyTaskRepository.save(task);
    }

    @Test
    void unauthenticatedPersonalApisAreRejectedButPublicNewsRemainsReadable() throws Exception {
        mockMvc.perform(get("/api/profile")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/bookmarks")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/learning/roadmap")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/news/latest")).andExpect(status().isOk());
    }

    @Test
    void registrationValidatesInputAndPreventsDuplicateCaseInsensitiveEmail() throws Exception {
        Csrf first = csrf(null);
        mockMvc.perform(post("/api/auth/register")
                        .session(first.session())
                        .header("X-XSRF-TOKEN", first.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("First User", "Duplicate@Example.com", PASSWORD, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("duplicate@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        Csrf second = csrf(null);
        mockMvc.perform(post("/api/auth/register")
                        .session(second.session())
                        .header("X-XSRF-TOKEN", second.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Second User", "duplicate@example.com", PASSWORD, PASSWORD)))
                .andExpect(status().isConflict());

        Csrf weak = csrf(null);
        mockMvc.perform(post("/api/auth/register")
                        .session(weak.session())
                        .header("X-XSRF-TOKEN", weak.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Weak User", "weak@example.com", "short", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").exists());

        Csrf invalidEmail = csrf(null);
        mockMvc.perform(post("/api/auth/register")
                        .session(invalidEmail.session())
                        .header("X-XSRF-TOKEN", invalidEmail.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Invalid User", "not-an-email", PASSWORD, PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginLogoutAndSessionRestorationWork() throws Exception {
        MockHttpSession session = register("Session User", "session@example.com");

        // Session context is persisted and can be read on a later request.
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("session@example.com"));

        Csrf logoutCsrf = csrf(session);
        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .header("X-XSRF-TOKEN", logoutCsrf.token()))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));

        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());

        Csrf loginCsrf = csrf(null);
        String preLoginSessionId = loginCsrf.session().getId();
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .session(loginCsrf.session())
                        .header("X-XSRF-TOKEN", loginCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"SESSION@example.com\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("session@example.com"))
                .andReturn();
        MockHttpSession authenticated = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(authenticated.getId()).isNotEqualTo(preLoginSessionId);
    }

    @Test
    void invalidCredentialsAreGenericAndRepeatedFailuresAreThrottled() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            Csrf csrf = csrf(null);
            mockMvc.perform(post("/api/auth/login")
                            .session(csrf.session())
                            .header("X-XSRF-TOKEN", csrf.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"throttle@example.com\",\"password\":\"WrongPass1!\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        Csrf blocked = csrf(null);
        mockMvc.perform(post("/api/auth/login")
                        .session(blocked.session())
                        .header("X-XSRF-TOKEN", blocked.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"throttle@example.com\",\"password\":\"WrongPass1!\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void csrfIsRequiredForCookieAuthenticatedMutations() throws Exception {
        MockHttpSession session = register("Csrf User", "csrf@example.com");

        mockMvc.perform(put("/api/profile")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Changed\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotReadOrModifyAnotherUsersRecordsById() throws Exception {
        MockHttpSession alice = register("Alice", "alice@example.com");
        MockHttpSession bob = register("Bob", "bob@example.com");

        Csrf aliceCsrf = csrf(alice);
        MvcResult added = mockMvc.perform(post("/api/bookmarks")
                        .session(alice)
                        .header("X-XSRF-TOKEN", aliceCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemType\":\"ARTICLE\",\"itemId\":" + article.getId() + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        long bookmarkId = objectMapper.readTree(added.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/bookmarks").session(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/bookmarks").session(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        Csrf bobCsrf = csrf(bob);
        mockMvc.perform(delete("/api/bookmarks/" + bookmarkId)
                        .session(bob)
                        .header("X-XSRF-TOKEN", bobCsrf.token()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/bookmarks").session(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        Csrf aliceUpdate = csrf(alice);
        mockMvc.perform(put("/api/profile")
                        .session(alice)
                        .header("X-XSRF-TOKEN", aliceUpdate.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Updated"));

        mockMvc.perform(get("/api/profile").session(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bob"));
    }

    @Test
    void learningProgressAndDailyTasksAreIsolatedByOwner() throws Exception {
        MockHttpSession alice = register("Alice Learn", "alice.learn@example.com");
        MockHttpSession bob = register("Bob Learn", "bob.learn@example.com");

        Csrf aliceCsrf = csrf(alice);
        mockMvc.perform(post("/api/learning/progress")
                        .session(alice)
                        .header("X-XSRF-TOKEN", aliceCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topicId\":" + topic.getId() + ",\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/learning/progress").session(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/learning/progress").session(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        Csrf taskCsrf = csrf(alice);
        mockMvc.perform(post("/api/learning/daily-plan/" + task.getId())
                        .session(alice)
                        .header("X-XSRF-TOKEN", taskCsrf.token())
                        .param("completed", "true"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/learning/daily-plan").session(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].tasks[0].completedInPlan").value(false));
    }

    @Test
    void readingHistoryIsRecordedOnlyForTheAuthenticatedReader() throws Exception {
        MockHttpSession alice = register("Alice History", "alice.history@example.com");
        MockHttpSession bob = register("Bob History", "bob.history@example.com");

        mockMvc.perform(get("/api/news/" + article.getId()).session(alice))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/profile").session(alice))
                .andExpect(jsonPath("$.articlesRead").value(1));
        mockMvc.perform(get("/api/profile").session(bob))
                .andExpect(jsonPath("$.articlesRead").value(0));
    }

    @Test
    void adminEndpointsRequireAdminRoleOrAdminKey() throws Exception {
        when(adminService.refreshNews()).thenReturn(new RefreshResultDto(
                Instant.now(), 0, 0, 0, 0, 0, 0, 0, List.of()));
        MockHttpSession user = register("Normal User", "normal.admin@example.com");

        Csrf csrf = csrf(user);
        mockMvc.perform(post("/api/admin/news/refresh")
                        .session(user)
                        .header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/news/refresh")
                        .header("X-Admin-Key", "test-admin-key"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/news/refresh")
                        .header("X-Admin-Key", "wrong-key"))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpSession register(String name, String email) throws Exception {
        Csrf csrf = csrf(null);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .session(csrf.session())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(name, email, PASSWORD, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }

    private Csrf csrf(MockHttpSession session) throws Exception {
        var builder = get("/api/auth/csrf");
        if (session != null) {
            builder.session(session);
        }
        MvcResult result = mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession used = (MockHttpSession) result.getRequest().getSession(false);
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new Csrf(used, body.get("token").asText());
    }

    private String registerJson(String name, String email, String password, String confirmPassword) {
        return "{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"password\":\""
                + password + "\",\"confirmPassword\":\"" + confirmPassword + "\"}";
    }

    private record Csrf(MockHttpSession session, String token) {
    }
}
