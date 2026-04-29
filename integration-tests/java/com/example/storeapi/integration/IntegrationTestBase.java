package com.example.storeapi.integration;

import com.example.storeapi.model.Product;
import com.example.storeapi.repository.ProductRepository;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.example.storeapi.StoreApiApplication.class)
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("storedb_test")
            .withUsername("testuser")
            .withPassword("testpassword")
            .withStartupTimeout(java.time.Duration.ofSeconds(120));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.sql.init.mode", () -> "never");
        registry.add("spring.session.store-type", () -> "jdbc");
        registry.add("spring.session.jdbc.initialize-schema", () -> "always");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "20000");
        registry.add("spring.datasource.hikari.validation-timeout", () -> "3000");
        registry.add("spring.datasource.hikari.max-lifetime", () -> "60000");
    }

    protected void seedProducts() {
        if (productRepository.count() == 0) {
            Product p1 = new Product();
            p1.setTitle("Nail gun");
            p1.setAvailable(8);
            p1.setPrice(new BigDecimal("23.95"));

            Product p2 = new Product();
            p2.setTitle("Hammer");
            p2.setAvailable(15);
            p2.setPrice(new BigDecimal("12.50"));

            Product p3 = new Product();
            p3.setTitle("Screwdriver set");
            p3.setAvailable(20);
            p3.setPrice(new BigDecimal("34.99"));

            Product p4 = new Product();
            p4.setTitle("Power drill");
            p4.setAvailable(5);
            p4.setPrice(new BigDecimal("89.95"));

            productRepository.saveAll(List.of(p1, p2, p3, p4));
        }
    }

    protected Cookie registerAndLogin(String email, String password) throws Exception {
        seedProducts();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                            {
                                "email": "%s",
                                "password": "%s"
                            }
                            """, email, password)))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                            {
                                "email": "%s",
                                "password": "%s"
                            }
                            """, email, password)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("SESSION");
        if (sessionCookie == null) {
            throw new IllegalStateException("Login did not return a SESSION cookie");
        }

        return sessionCookie;
    }
}