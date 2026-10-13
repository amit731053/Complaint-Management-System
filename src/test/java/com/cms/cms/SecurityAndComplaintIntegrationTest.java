package com.cms.cms;

import com.cms.cms.model.User;
import com.cms.cms.repository.ComplaintRepository;
import com.cms.cms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAndComplaintIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ComplaintRepository complaints;
    @Autowired PasswordEncoder encoder;

    static final String GOOD = """
            {"title":"Wifi down","description":"No internet in lab 3","category":"it_network","priority":"high"}""";

    @BeforeEach
    void clean() {
        complaints.deleteAll();
        users.findAll().stream()
                .filter(u -> !"ADMIN".equals(u.getRole()))
                .forEach(users::delete);
        makeUser("a@test.local");
        makeUser("b@test.local");
    }

    private void makeUser(String email) {
        User u = new User();
        u.setName(email);
        u.setEmail(email);
        u.setPassword(encoder.encode("Passw0rd1"));
        u.setRole("USER");
        users.save(u);
    }

    private long createAs(String email) throws Exception {
        MvcResult r = mvc.perform(post("/api/complaints").with(user(email).roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(GOOD))
                .andExpect(status().isCreated()).andReturn();
        String body = r.getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    // ---------- authentication ----------

    @Test void anonymousCannotListComplaints() throws Exception {
        mvc.perform(get("/api/complaints")).andExpect(status().isUnauthorized());
    }

    @Test void anonymousCannotDeleteOrListUsers() throws Exception {
        mvc.perform(delete("/api/complaints/1").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    }

    @Test void removedDangerousEndpointsAreGone() throws Exception {
        mvc.perform(get("/api/complaints/test-user")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/complaints/forgot-password").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"a@test.local\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test void writeWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/api/complaints").with(user("a@test.local").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(GOOD))
                .andExpect(status().isForbidden());
    }

    @Test void signupHashesPasswordAndIgnoresRole() throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New\",\"email\":\"New@Test.local\",\"password\":\"Passw0rd1\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated());
        User u = users.findByEmail("new@test.local").orElseThrow();
        assertEquals("USER", u.getRole());
        assertTrue(u.getPassword().startsWith("$2"), "password must be a BCrypt hash");
    }

    @Test void signupRejectsDuplicateAndWeakPassword() throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"email\":\"a@test.local\",\"password\":\"Passw0rd1\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"email\":\"c@test.local\",\"password\":\"weak\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test void loginNeverReturnsPassword() throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"A@test.local\",\"password\":\"Passw0rd1\"}"))
                .andExpect(status().isOk()).andReturn();
        String body = r.getResponse().getContentAsString();
        assertFalse(body.toLowerCase().contains("password"));
        assertTrue(body.contains("\"role\":\"USER\""));
    }

    @Test void wrongPasswordIs401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@test.local\",\"password\":\"WrongPass1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test void tooManyFailedLoginsAreBlockedEvenWithCorrectPassword() throws Exception {
        makeUser("rl@test.local");
        String bad = "{\"email\":\"rl@test.local\",\"password\":\"WrongPass1\"}";
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(bad))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"rl@test.local\",\"password\":\"Passw0rd1\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test void healthEndpointIsPublic() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    @Test void meReturns401WhenNotLoggedIn() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test void seededAdminCanLogin() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.local\",\"password\":\"Admin@12345\"}"))
                .andExpect(status().isOk());
    }

    // ---------- authorization / ownership ----------

    @Test void userCannotReadOrEditSomeoneElsesComplaint() throws Exception {
        long id = createAs("a@test.local");
        mvc.perform(get("/api/complaints/" + id).with(user("b@test.local").roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/complaints/" + id + "/edit").with(user("b@test.local").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(GOOD))
                .andExpect(status().isForbidden());
    }

    @Test void listOnlyShowsOwnComplaintsForUsersAndAllForAdmin() throws Exception {
        createAs("a@test.local");
        createAs("b@test.local");
        String a = mvc.perform(get("/api/complaints").with(user("a@test.local").roles("USER")))
                .andReturn().getResponse().getContentAsString();
        assertEquals(1, a.split("\"id\":").length - 1);
        String admin = mvc.perform(get("/api/complaints").with(user("admin@test.local").roles("ADMIN")))
                .andReturn().getResponse().getContentAsString();
        assertEquals(2, admin.split("\"id\":").length - 1);
        assertFalse(admin.toLowerCase().contains("password"));
    }

    @Test void userCannotDeleteCommentOrListUsers() throws Exception {
        long id = createAs("a@test.local");
        var u = user("a@test.local").roles("USER");
        mvc.perform(delete("/api/complaints/" + id).with(u).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/complaints/" + id + "/comment").with(u).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"hi\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").with(u)).andExpect(status().isForbidden());
    }

    @Test void adminCanCommentChangeStatusDeleteAndListUsersWithoutPasswords() throws Exception {
        long id = createAs("a@test.local");
        var admin = user("admin@test.local").roles("ADMIN");
        mvc.perform(post("/api/complaints/" + id + "/comment").with(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"Working on it\"}")).andExpect(status().isOk());
        mvc.perform(put("/api/complaints/" + id + "?status=IN_PROGRESS").with(admin).with(csrf())).andExpect(status().isOk());
        String users = mvc.perform(get("/api/admin/users").with(admin)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(users.toLowerCase().contains("password"));
        mvc.perform(delete("/api/complaints/" + id).with(admin).with(csrf())).andExpect(status().isNoContent());
    }

    @Test void userMayOnlyResolveOwnComplaint() throws Exception {
        long id = createAs("a@test.local");
        var a = user("a@test.local").roles("USER");
        mvc.perform(put("/api/complaints/" + id + "?status=IN_PROGRESS").with(a).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(put("/api/complaints/" + id + "?status=BOGUS").with(a).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(put("/api/complaints/" + id + "?status=RESOLVED").with(user("b@test.local").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/complaints/" + id + "?status=RESOLVED").with(a).with(csrf())).andExpect(status().isOk());
    }

    @Test void missingComplaintIs404() throws Exception {
        mvc.perform(get("/api/complaints/999999").with(user("a@test.local").roles("USER")))
                .andExpect(status().isNotFound());
    }

    // ---------- validation ----------

    @Test void htmlInTitleDescriptionOrCommentIsRejected() throws Exception {
        var a = user("a@test.local").roles("USER");
        mvc.perform(post("/api/complaints").with(a).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"<script>x</script>\",\"description\":\"d\",\"category\":\"other\",\"priority\":\"low\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/complaints").with(a).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"ok\",\"description\":\"<img onerror=x>\",\"category\":\"other\",\"priority\":\"low\"}"))
                .andExpect(status().isBadRequest());
        long id = createAs("a@test.local");
        mvc.perform(post("/api/complaints/" + id + "/comment").with(user("admin@test.local").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"<b>x</b>\"}")).andExpect(status().isBadRequest());
    }

    @Test void invalidPriorityCategoryAndHugeInputRejected() throws Exception {
        var a = user("a@test.local").roles("USER");
        mvc.perform(post("/api/complaints").with(a).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"ok\",\"description\":\"d\",\"category\":\"nope\",\"priority\":\"low\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/complaints").with(a).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"ok\",\"description\":\"d\",\"category\":\"other\",\"priority\":\"urgent\"}"))
                .andExpect(status().isBadRequest());
        String huge = "x".repeat(2001);
        mvc.perform(post("/api/complaints").with(a).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"ok\",\"description\":\"" + huge + "\",\"category\":\"other\",\"priority\":\"low\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test void ownerAndStatusAreNotTakenFromClient() throws Exception {
        MvcResult r = mvc.perform(post("/api/complaints").with(user("a@test.local").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Mine\",\"description\":\"d\",\"category\":\"other\",\"priority\":\"low\","
                                + "\"status\":\"RESOLVED\",\"userEmail\":\"b@test.local\"}"))
                .andExpect(status().isCreated()).andReturn();
        String body = r.getResponse().getContentAsString();
        assertTrue(body.contains("\"status\":\"PENDING\""));
        assertTrue(body.contains("\"userEmail\":\"a@test.local\""));
    }
}
