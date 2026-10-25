package ru.studentlab.students;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.studentlab.students.service.port.AdministratorAccounts;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.datasource.url=jdbc:h2:mem:solid-api;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=solid-tests-secret-key-at-least-32-bytes-long",
        "app.jwt.access-token-expiration=900000", "app.jwt.refresh-token-expiration=604800000",
        "app.admin.username=test-admin", "app.admin.email=admin@test.example", "app.admin.password=admin123"
})
@AutoConfigureMockMvc
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AdministratorAccounts administrators;
    private String admin;

    @BeforeEach
    void loginAsAdmin() throws Exception {
        admin = login("test-admin", "admin123").get("accessToken").asText();
    }

    @Test
    void registrationLoginProfileRefreshAndPermissionsKeepTheirContract() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"username\":\" reader \",\"email\":\"READER@test.example\",\"password\":\"reader123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated());
        JsonNode tokens = login("reader", "reader123");
        String access = tokens.get("accessToken").asText();
        assertEquals("Bearer", tokens.get("tokenType").asText());
        assertEquals(900, tokens.get("expiresIn").asLong());
        mvc.perform(auth(get("/api/auth/me"), access)).andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("reader"))
                .andExpect(jsonPath("$.role").value("USER"));
        mvc.perform(auth(get("/api/requests"), access)).andExpect(status().isOk());
        mvc.perform(auth(post("/api/requests"), access).contentType(MediaType.APPLICATION_JSON).content(student(990101)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.message").exists());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of("refreshToken", tokens.get("refreshToken").asText()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isString());
        mvc.perform(auth(get("/api/requests"), tokens.get("refreshToken").asText()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of("refreshToken", access))))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/requests")).andExpect(status().isUnauthorized());
        mvc.perform(auth(get("/api/requests"), "broken.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crudKeepsPutPatchUniquenessPaginationAndLegacySearch() throws Exception {
        mvc.perform(auth(post("/api/requests"), admin).contentType(MediaType.APPLICATION_JSON).content(student(990201)))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/requests/990201"));
        mvc.perform(auth(post("/api/requests"), admin).contentType(MediaType.APPLICATION_JSON).content(student(990202)))
                .andExpect(status().isCreated());
        mvc.perform(auth(post("/api/requests"), admin).contentType(MediaType.APPLICATION_JSON).content(student(990201)))
                .andExpect(status().isConflict());
        mvc.perform(auth(patch("/api/requests/990201"), admin).contentType(MediaType.APPLICATION_JSON).content("{\"notes\":\"keep\",\"room\":\"202\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Иван Иванов"));
        mvc.perform(auth(patch("/api/requests/990201"), admin).contentType(MediaType.APPLICATION_JSON).content("{\"isu\":990202}"))
                .andExpect(status().isConflict());
        mvc.perform(auth(patch("/api/requests/990201"), admin).contentType(MediaType.APPLICATION_JSON).content("{\"room\":\"303\",\"dorm\":0}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(auth(get("/api/requests/990201"), admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.room").value("202")).andExpect(jsonPath("$.notes").value("keep"));
        mvc.perform(auth(put("/api/requests/990201"), admin).contentType(MediaType.APPLICATION_JSON).content(student(990201)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.notes").value(""));
        mvc.perform(auth(get("/api/requests").param("isu", "99020").param("size", "1").param("sort", "isu,desc"), admin))
                .andExpect(status().isOk()).andExpect(header().string("X-Total-Count", "2"))
                .andExpect(header().string("X-Page", "0")).andExpect(header().string("X-Page-Size", "1"))
                .andExpect(jsonPath("$[0].isu").value(990202));
        mvc.perform(auth(request("QUERY", java.net.URI.create("/api/requests")), admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"dormitory\":8,\"isu\":\"99020\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(auth(delete("/api/requests/990201"), admin)).andExpect(status().isNoContent());
        mvc.perform(auth(delete("/api/requests/990202"), admin)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/requests/990201"), admin)).andExpect(status().isNotFound());
    }

    @Test
    void invalidInputKeepsErrorStatusAndEnvelope() throws Exception {
        mvc.perform(auth(post("/api/requests"), admin).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.details").isMap());
        mvc.perform(auth(post("/api/requests"), admin).contentType(MediaType.APPLICATION_JSON).content("{\"isu\":1}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.error.details.isu").exists());
        mvc.perform(auth(get("/api/requests").param("sort", "name;drop table students"), admin))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test-admin\",\"password\":\"incorrect\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test-admin\",\"email\":\"other@test.example\",\"password\":\"admin123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void adminInitializationIsIdempotentAndUpdatesPassword() throws Exception {
        administrators.initialize("test-admin", "admin@test.example", "admin123");
        administrators.initialize("test-admin", "admin@test.example", "new-password");
        try {
            login("test-admin", "new-password");
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"test-admin\",\"password\":\"admin123\"}"))
                    .andExpect(status().isUnauthorized());
        } finally {
            administrators.initialize("test-admin", "admin@test.example", "admin123");
        }
    }

    private JsonNode login(String username, String password) throws Exception {
        String body = json.writeValueAsString(java.util.Map.of("username", username, "password", password));
        return json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    private String student(int isu) {
        return "{\"name\":\"Иван Иванов\",\"group\":\"P3220\",\"isu\":\"" + isu
                + "\",\"dorm\":\"8\",\"room\":\"101\",\"expDate\":\"2027-06-01\",\"foreigner\":false}";
    }
}
