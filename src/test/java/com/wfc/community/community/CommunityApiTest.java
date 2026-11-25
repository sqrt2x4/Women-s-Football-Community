package com.wfc.community.community;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommunityApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndListsCommunities() throws Exception {
        mockMvc.perform(post("/api/communities")
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

        mockMvc.perform(get("/api/communities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Arsenal Women"));
    }

    @Test
    void rejectsInvalidAndDuplicateCommunities() throws Exception {
        String request = """
                {
                  "name": "Chelsea Women",
                  "slug": "chelsea-women",
                  "description": "Chelsea supporters."
                }
                """;
        mockMvc.perform(post("/api/communities").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/communities").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/communities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","slug":"Not A Slug","description":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void joinsCommunityPublishesPostAndAddsComment() throws Exception {
        String community = mockMvc.perform(post("/api/communities")
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
        String communityId = com.jayway.jsonpath.JsonPath.read(community, "$.id");

        mockMvc.perform(post("/api/communities/" + communityId + "/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorName":"Alex","content":"Up the Thorns!"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/communities/" + communityId + "/memberships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Alex"}
                                """))
                .andExpect(status().isCreated());

        String post = mockMvc.perform(post("/api/communities/" + communityId + "/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorName":"Alex","content":"Up the Thorns!"}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String postId = com.jayway.jsonpath.JsonPath.read(post, "$.id");

        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorName":"Sam","content":"What a match!"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/communities/" + communityId + "/memberships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Sam"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorName":"Sam","content":"What a match!"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("What a match!"));

        mockMvc.perform(get("/api/communities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].memberCount").value(2));

        mockMvc.perform(delete("/api/communities/" + communityId + "/memberships")
                        .param("displayName", "Sam"))
                .andExpect(status().isNoContent());
    }
}
