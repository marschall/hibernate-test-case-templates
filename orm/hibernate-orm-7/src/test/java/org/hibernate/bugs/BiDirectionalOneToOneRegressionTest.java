package org.hibernate.bugs;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static jakarta.persistence.SharedCacheMode.NONE;
import static org.junit.jupiter.api.Assertions.*;

@DomainModel(
        annotatedClasses = {Author.class, Person.class, Employer.class, EmploymentLink.class},
        sharedCacheMode = NONE)
@ServiceRegistry(settings = {
        @Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
        @Setting(name = AvailableSettings.FORMAT_SQL, value = "true"),
        @Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false"),
})
public class BiDirectionalOneToOneRegressionTest {

    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void init() {
        entityManagerFactory = Persistence.createEntityManagerFactory( "templatePU" );
    }

    @AfterEach
    void destroy() {
        entityManagerFactory.close();
    }

    @Test
    void hhh123Test() {
        Long personWithoutAuthorId;
        Long personWithAuthorId;
        Long employmentId;

        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {

            entityManager.getTransaction().begin();

            Employer employment = new Employer();
            entityManager.persist(employment);

            Person personWithoutAuthor = new Person();
            entityManager.persist(personWithoutAuthor);
            EmploymentLink link1 = new EmploymentLink(personWithoutAuthor, employment);
            entityManager.persist(link1);

            Person personWithAuthor = new Person();
            Author author = new Author();
            author.setPerson(personWithAuthor);
            personWithAuthor.setAuthor(author);
            entityManager.persist(personWithAuthor);
            entityManager.persist(author);

            EmploymentLink link2 = new EmploymentLink(personWithAuthor, employment);
            entityManager.persist(link2);

            entityManager.getTransaction().commit();

            personWithoutAuthorId = personWithoutAuthor.getId();
            personWithAuthorId = personWithAuthor.getId();
            employmentId =  employment.getId();
        }

        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {

            entityManager.getTransaction().begin();

            Employer employment = entityManager.find(Employer.class, employmentId);
            assertNotNull(employment);
            List<Person> persons = employment.getPersons();
            assertEquals(2, persons.size());

            /*
            Person person = entityManager.find(Person.class, personWithoutAuthorId);
            assertNotNull(person);
            assertNull(person.getAuthor());

            person = entityManager.find(Person.class, personWithAuthorId);
            assertNotNull(person);
            assertNotNull(person.getAuthor(), "author");
             */

            entityManager.getTransaction().setRollbackOnly();
        }
    }
}
