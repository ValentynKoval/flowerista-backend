package ua.flowerista.shop.repo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.ClassRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import ua.flowerista.shop.config.PostgresTestProfileJPAConfig;
import ua.flowerista.shop.models.Flower;

@SpringBootTest(classes = PostgresTestProfileJPAConfig.class)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class FlowerRepositoryTest extends AbstractTransactionalJUnit4SpringContextTests {

	@Autowired
	private FlowerRepository repository;

	private final static String SCRIPT_DB = "db.sql";

	@Container
	@ClassRule
	public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:11-alpine")
			.withDatabaseName("integration-tests-db").withPassword("inmemory").withUsername("inmemory")
			.withInitScript(SCRIPT_DB);

	@DynamicPropertySource
	static void postgresqlProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
		registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
		registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
	}

	@AfterAll
	public static void stopContainer() {
		postgreSQLContainer.stop();
	}

	@Test
	void testInsertFlower() {
		Flower expected = new Flower();
		expected.setName("123");
		repository.save(expected);
		Flower actual = repository.getReferenceById(repository.findAll().size());
		assertEquals(expected.getName(), actual.getName());
	}

	@Test
	void testDeleteById() {
		List<Flower> flowers = repository.findAll();
		repository.deleteById(flowers.get(flowers.size()-1).getId());
		int expected = 3;
		int actual = repository.findAll().size();
		assertEquals(expected, actual);
	}

	@Test
	void testGetAllFlowers() {
		int expected = 4;
		int actual = repository.findAll().size();
		assertEquals(expected, actual);
	}

	@Test
	void testGetById() {
		Flower expected = repository.findAll().get(1);
		Flower actual = repository.getReferenceById(repository.findAll().get(1).getId());
		assertEquals(expected, actual);
	}

	@Test
	void testupdate() {
		Flower expected = repository.findAll().get(1);
		int colorId = expected.getId();
		expected.setName("55");
		repository.save(expected);
		Flower actual = repository.getReferenceById(colorId);
		assertEquals(expected, actual);
	}

}
