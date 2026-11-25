package com.wfc.community.community;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommunityApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registersAndAuthenticatesAccountsWithHashedPasswords() throws Exception {
        MockHttpSession session = register("alex@example.com", "Alex");

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alex@example.com"))
                .andExpect(jsonPath("$.displayName").value("Alex"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ALEX@example.com","displayName":"Another Alex","password":"password123"}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alex@example.com","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"short-password@example.com","displayName":"Fan","password":"short"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/logout").with(csrf()).session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ALEX@EXAMPLE.COM","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Alex"))
                .andReturn();
        MockHttpSession loggedInSession = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/me").session(loggedInSession))
                .andExpect(status().isOk());
    }

    @Test
    void requiresAuthenticationForCommunityWrites() throws Exception {
        mockMvc.perform(get("/api/communities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/communities/memberships/mine"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/communities")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Arsenal Women",
                                  "slug": "arsenal-women",
                                  "description": "Supporters of Arsenal Women."
                                }
                                """))
                .andExpect(status().isUnauthorized());

        MockHttpSession session = register("creator@example.com", "Creator");
        mockMvc.perform(post("/api/communities")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Arsenal Women",
                                  "slug": "arsenal-women",
                                  "description": "Supporters of Arsenal Women."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.slug").value("arsenal-women"));
    }

    @Test
    void validatesCommunityCreationAndRejectsDuplicateSlugs() throws Exception {
        MockHttpSession session = register("fan@example.com", "Fan");
        String request = """
                {
                  "name": "Chelsea Women",
                  "slug": "chelsea-women",
                  "description": "Chelsea supporters."
                }
                """;

        mockMvc.perform(post("/api/communities")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/communities")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/communities")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","slug":"Not A Slug","description":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void membershipAndContentBelongToAuthenticatedUsers() throws Exception {
        MockHttpSession alex = register("alex@example.com", "Alex");
        MockHttpSession sam = register("sam@example.com", "Sam");
        String communityId = createCommunity(alex);

        mockMvc.perform(post("/api/communities/" + communityId + "/memberships")
                        .session(alex))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/communities/" + communityId + "/posts")
                        .with(csrf())
                        .session(alex)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"A post before joining"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/communities/" + communityId + "/memberships")
                        .with(csrf())
                        .session(alex))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayName").value("Alex"));

        mockMvc.perform(post("/api/communities/" + communityId + "/memberships")
                        .with(csrf())
                        .session(alex))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/communities/" + communityId + "/posts")
                        .with(csrf())
                        .session(alex)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorName":"Impersonated","content":"Up the Thorns!"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorName").value("Alex"));

        String postId = mockMvc.perform(get("/api/communities/" + communityId + "/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(postId, "$[0].id");

        mockMvc.perform(post("/api/posts/" + id + "/comments")
                        .with(csrf())
                        .session(sam)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Should not comment before joining"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/communities/" + communityId + "/memberships")
                        .with(csrf())
                        .session(sam))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/posts/" + id + "/comments")
                        .with(csrf())
                        .session(sam)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorName":"Forged","content":"What a match!"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorName").value("Sam"))
                .andExpect(jsonPath("$.content").value("What a match!"));

        mockMvc.perform(get("/api/communities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].memberCount").value(2));

        mockMvc.perform(get("/api/communities/memberships/mine").session(alex))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(delete("/api/communities/" + communityId + "/memberships")
                        .with(csrf())
                        .session(sam))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/communities/memberships/mine").session(alex))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    private MockHttpSession register(String email, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","displayName":"%s","password":"password123"}
                                """.formatted(email, displayName)))
                .andExpect(status().isCreated())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String createCommunity(MockHttpSession session) throws Exception {
        String response = mockMvc.perform(post("/api/communities")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Portland Thorns",
                                  "slug": "portland-thorns",
                                  "description": "Thorns supporters."
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }
}
