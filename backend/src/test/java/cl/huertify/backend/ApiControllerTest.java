package cl.huertify.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerTest {
    @Autowired MockMvc mvc;

    @Test
    void loginReturnsUserWithoutPasswordHash() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@huertify.cl\",\"password\":\"huerto123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@huertify.cl"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void invalidLoginIsUnauthorized() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@huertify.cl\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sectorCrudSmokeTestAndUnknownIdsAreNotFound() throws Exception {
        String created = mvc.perform(post("/api/sectors").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"TEST-1\",\"name\":\"Prueba\",\"location\":\"Patio\",\"areaM2\":3.5," +
                                "\"description\":\"Sector de prueba\",\"status\":\"DISPONIBLE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn().getResponse().getContentAsString();
        long id = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(created).get("id").asLong();

        mvc.perform(put("/api/sectors/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"TEST-1\",\"name\":\"Prueba editada\",\"location\":\"Patio\",\"areaM2\":4.0," +
                                "\"description\":\"Actualizado\",\"status\":\"EN_USO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Prueba editada"));
        mvc.perform(delete("/api/sectors/" + id)).andExpect(status().isNoContent());
        mvc.perform(put("/api/sectors/999999").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/sectors/999999")).andExpect(status().isNotFound());
    }
}
